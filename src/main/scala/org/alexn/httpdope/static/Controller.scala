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

package org.alexn.httpdope.static

import cats.implicits._
import cats.effect.{Blocker, ContextShift, Sync}
import org.alexn.httpdope.config.HttpServerConfig
import org.alexn.httpdope.utils.{BaseController, HttpUtils}
import org.http4s.{HttpRoutes, Request, Response, StaticFile}
import scala.concurrent.duration._

final class Controller[F[_]](cfg: HttpServerConfig, blocker: Blocker)
  (implicit F: Sync[F], cs: ContextShift[F])
  extends BaseController[F] {

  def routes =
    HttpRoutes.of[F] {
      case request @ GET -> Root =>
        val rootURL = HttpUtils.getRootURL(request).getOrElse("//" + cfg.canonicalDomain)
        Ok(html.index(rootURL)).map(HttpUtils.cached(1.hour))

      case request @ GET -> Root / "favicon.ico" =>
        serveStaticFile("images/favicon.ico", request)

      case request @ GET -> "public" /: ValidPath(path) =>
        serveStaticFile(path, request)
    }

  def serveStaticFile(resourcePath: String, r: Request[F]): F[Response[F]] =
    StaticFile.fromResource(s"/public/$resourcePath", blocker, Some(r)).value.map {
      case Some(r) => HttpUtils.cached(4.hours)(r)
      case None => Response.notFound[F]
    }

  object ValidPath {
    def unapply(ref: Path): Option[String] =
      ref.toList match {
        case Nil | ("" | "/") :: Nil => None
        case other if !other.contains("..") => Some(other.mkString("/"))
        case _ => None
      }
  }
}

object Controller {
  def apply[F[_]](cfg: HttpServerConfig, blocker: Blocker)(implicit F: Sync[F], cs: ContextShift[F]) =
    new Controller[F](cfg, blocker)
}
