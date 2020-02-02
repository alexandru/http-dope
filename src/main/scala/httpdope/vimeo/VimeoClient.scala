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

import cats.implicits._
import cats.data.EitherT
import cats.effect.{Clock, Concurrent, Sync}
import httpdope.common.models.{HttpError, JSONError, WebError}
import httpdope.common.utils.{Cached, StrictLogging}
import io.circe.Decoder
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.headers.Accept
import org.http4s.util.CaseInsensitiveString
import org.http4s._
import scala.concurrent.duration._

final class VimeoClient[F[_]] private (accessToken: VimeoAccessToken, cache: Cached[F, Either[WebError, AnyRef]], client: Client[F])
  (implicit F: Sync[F])
  extends Http4sClientDsl[F] with StrictLogging {

  /**
    * Fetches the download links from Vimeo for a public video with the
    * given `uid` and for which the download is allowed (Plus accounts and up).
    */
  def getDownloadLinks(uid: String, exp: FiniteDuration, agent: Option[Header], extra: Option[Header]*): EitherT[F, WebError, DownloadLinksJSON] =
    EitherT(
      cache
        .getOrUpdate(uid + "/links/" + exp.toMillis.toString, exp, uncachedDownloadLinks(uid, agent, extra:_*).widen[AnyRef].value)
        .asInstanceOf[F[Either[WebError, DownloadLinksJSON]]]
    )

  /**
    * Fetches thumbnail links.
    */
  def getPictures(uid: String, exp: FiniteDuration, agent: Option[Header], extra: Option[Header]*): EitherT[F, WebError, VimeoConfigJSON] =
    EitherT(
      cache
        .getOrUpdate(uid + "/pictures/" + exp.toMillis.toString, exp, uncachedPictures(uid, agent, extra:_*).widen[AnyRef].value)
        .asInstanceOf[F[Either[WebError, VimeoConfigJSON]]]
    )

  private def uncachedPictures(uid: String, agent: Option[Header], extra: Option[Header]*): EitherT[F, WebError, VimeoConfigJSON] =
    uncachedGET[VimeoConfigJSON](uid, s"https://api.vimeo.com/videos/$uid?access_token=${accessToken.value}&per_page=100", agent, extra:_*)

  private def uncachedDownloadLinks(uid: String, agent: Option[Header], extra: Option[Header]*): EitherT[F, WebError, DownloadLinksJSON] =
    uncachedGET[DownloadLinksJSON](uid, s"https://vimeo.com/$uid?action=load_download_config", agent, extra:_*)

  private def uncachedGET[A](uid: String, url: String, agent: Option[Header], extra: Option[Header]*)
    (implicit ev: Decoder[A]): EitherT[F, WebError, A] = {

    import org.http4s.circe.CirceEntityDecoder._
    val base = Method.GET(
      Uri.unsafeFromString(url),
      Accept(MediaType.application.json)
    )

    EitherT(base.flatMap { req =>
      val request = req
        .putHeaders(
          Header("origin", "https://vimeo.com"),
          agent.getOrElse(Header("User-Agent", "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:63.0) Gecko/20100101 Firefox/63.0")),
          Header("Referer", s"https://vimeo.com/$uid"),
          Header("x-requested-with", "XMLHttpRequest")
        )
        .putHeaders(extra.collect { case Some(h) => h } :_*)

      logger.info("Making request: " + request.toString())
      client.fetch(request) {
        case Status.Successful(r) if r.status.code == 200 =>
          r.attemptAs[A].leftMap(e => JSONError(e.message) : WebError).value

        case r =>
          val contentType = r.headers.get(CaseInsensitiveString("Content-Type")).map(_.value)
          val process = r.bodyAsText
            .compile
            .fold(new StringBuilder)((acc, e) => acc.append(e))
            .map(_.toString())

          for (body <- process) yield {
            Left(HttpError(r.status.code, body, contentType))
          }
      }
    })
  }
}

object VimeoClient {
  /**
    * Builds a [[VimeoClient]] resource.
    */
  def apply[F[_]](accessToken: VimeoAccessToken, client: Client[F])
    (implicit F: Concurrent[F], clock: Clock[F]): F[VimeoClient[F]] = {

    Cached[F, Either[WebError, AnyRef]].map { cache =>
      new VimeoClient(accessToken, cache, client)
    }
  }
}
