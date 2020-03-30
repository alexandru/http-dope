/*
 * Copyright 2020 Alexandru Nedelcu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package httpdope.vimeo

import cats.data.EitherT
import cats.effect.{Concurrent, Resource, Sync}
import cats.implicits._
import httpdope.common.http.{BaseController, ControllerLike, StaticController}
import httpdope.common.models.{HttpError, JSONError, WebError}
import httpdope.common.utils.{CacheManager, SystemCommands}
import httpdope.vimeo.models.CacheTTL.{LongTerm, NoCache, ShortTerm}
import httpdope.vimeo.models.{CacheTTL, DownloadLinksJSON, PicturesEntrySizeJSON, VimeoConfig, VimeoConfigJSON}
import io.circe.Encoder
import io.circe.syntax._
import org.http4s.client.Client
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, HttpRoutes, Request, Response, Status}

import scala.concurrent.duration._

final class Controller[F[_]](client: VimeoClient[F], system: SystemCommands[F])
  (implicit F: Sync[F]) extends BaseController[F] {

  type UserAgent = Header
  type ForwardedFor = Header

  val httpCacheExpiry = 1.day.toSeconds.toString

  override def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root / "redirect" / uid / name :? DownloadParam(download) =>
      findDownloads(request, uid, NoCache) { info =>
        if (info.allow_downloads) {
          val search = name.toLowerCase.trim
          val file = info.source_file
            .find(_.public_name.toLowerCase.trim == search)
            .orElse(info.files.find(_.public_name.toLowerCase.trim == search))

          file match {
            case None =>
              notFound("file", name)

            case Some(value) =>
              val url = cleanURL(value.download_url, download.getOrElse(true))
              logger.info("Serving (video): " + url)
              Response[F](Status.SeeOther)
                .putHeaders(Header("Location", url))
                .putHeaders(Header("Cache-Control", "no-cache"))
                .pure[F]
          }
        } else {
          notFound("allow_downloads", "false")
        }
      }

    case request @ GET -> Root / "get" / uid =>
      println(s"UID: $uid")
      findDownloads(request, uid, LongTerm)(jsonToResponse)

    case request @ GET -> Root / "config" / uid =>
      println("PUla")
      findThumbs(request, uid, ShortTerm)(jsonToResponse)

    case request @ GET -> Root / "thumb" / uid :? MinWidth(minWidth) =>
      thumbView(request, "thumb", uid, minWidth)(_.link)

    case request @ GET -> Root / "thumb-play" / uid :? MinWidth(minWidth) =>
      thumbView(request, "thumb-play", uid, minWidth) { p =>
        p.link_with_play_button.getOrElse(p.link)
      }
  }

  private def jsonToResponse[A : Encoder](response: A): F[Response[F]] =
    Response[F](Status.Ok)
      .withEntity(response.asJson)
      .putHeaders(Header("Cache-Control", "public, max-age=" + httpCacheExpiry))
      .pure[F]

  private def thumbView(request: Request[F], name: String, uid: String, minWidth: Option[Int])
    (f: PicturesEntrySizeJSON => String): F[Response[F]] = {

    findThumbs(request, uid, ShortTerm) { info =>
      info.pictures.sizes match {
        case Nil =>
          notFound(s"thumb/$name", Some(uid))

        case list =>
          val picture = minWidth match {
            case None =>
              list.maxBy(_.width)
            case Some(w) =>
              val sorted = list.sortBy(_.width)
              sorted.find(_.width >= w).getOrElse(sorted.last)
          }

          val url = f(picture)
          Response[F](Status.SeeOther)
            .putHeaders(Header("Location", url))
            .putHeaders(Header("Cache-Control", "public, max-age=" + httpCacheExpiry))
            .pure[F]
      }
    }
  }

  private def findThumbs(
    request: Request[F],
    uid: String,
    ttl: CacheTTL
  )(f: VimeoConfigJSON => F[Response[F]]): F[Response[F]] = {

    find(request, f) { (agent, forwardedFor) =>
      client.getPictures(uid, ttl, agent, forwardedFor)
    }
  }

  private def findDownloads(
    request: Request[F],
    uid: String,
    ttl: CacheTTL
  )(f: DownloadLinksJSON => F[Response[F]]): F[Response[F]] = {
    find(request, f) { (agent, forwardedFor) =>
      client.getDownloadLinks(uid, ttl, agent, forwardedFor)
    }
  }

  private def find[T](request: Request[F], f: T => F[Response[F]])
    (generate: (Option[UserAgent], Option[ForwardedFor]) => EitherT[F, WebError, T]): F[Response[F]] = {

    system.getServerIP.flatMap { serverIP =>
      val agent = request.headers.get(CaseInsensitiveString("User-Agent"))
      val currentForwardedFor =
        request.headers
          .get(CaseInsensitiveString("X-Forwarded-For"))
          .orElse(request.headers.get(CaseInsensitiveString("X-Client-IP")))
          .orElse(request.headers.get(CaseInsensitiveString("X-ProxyUser-Ip")))

      val forwardedFor = serverIP.filter(_.value.nonEmpty) match {
        case None => currentForwardedFor
        case Some(proxyIP) =>
          currentForwardedFor match {
            case None =>
              request.remoteAddr.map(
                ip => Header("X-Forwarded-For", ip + ", " + proxyIP.value)
              )
            case Some(header) =>
              Some(Header("X-Forwarded-For", header.value + ", " + proxyIP.value))
          }
      }

      generate(agent, forwardedFor).value.flatMap {
        case Left(HttpError(status, body, contentType)) =>
          // Core web-service triggered an HTTP error, mirroring it as is
          val r = Response[F](Status.fromInt(status).getOrElse(Status.InternalServerError)).withEntity(body)
          contentType.fold(r)(ct => r.putHeaders(Header("Content-Type", ct))).pure[F]

        case Left(JSONError(msg)) =>
          badGateway(msg)

        case Left(error) =>
          internalServerError(error.toString)

        case Right(info) =>
          f(info)
      }
    }
  }

  private def cleanURL(url: String, download: Boolean): String =
    if (download) {
      if (!url.contains("download"))
        url + "&download=1"
      else
        url
    } else {
      url.replaceAll("[&]download[=]\\w+", "")
    }

  private object DownloadParam
    extends OptionalQueryParamDecoderMatcher[Boolean]("download")

  private object MinWidth
    extends OptionalQueryParamDecoderMatcher[Int]("width")
}

object Controller {
  def apply[F[_]](
    config: VimeoConfig,
    cacheManager: CacheManager[F],
    client: Client[F],
    system: SystemCommands[F])
    (implicit F: Concurrent[F]): Resource[F, ControllerLike[F]] = {

    config.accessToken match {
      case None =>
        Resource.pure[F, ControllerLike[F]](
          StaticController[F].serviceUnavailable("Vimeo access token not configured")
        )
      case Some(token) =>
        for {
          client <- VimeoClient[F](token, config.cache, cacheManager, client)
        } yield {
          new Controller(client, system)
        }
    }
  }
}
