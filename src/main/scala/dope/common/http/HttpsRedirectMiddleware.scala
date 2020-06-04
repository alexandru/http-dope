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

package dope.common.http

import cats.implicits._
import cats.Applicative
import cats.data.Kleisli
import dope.common.utils.LazyLogging
import dope.config.HttpServerConfig
import org.http4s.Status.MovedPermanently
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.headers.{Host, Location, `Content-Type`}
import org.http4s.{Headers, Http, MediaType, Response}

object HttpsRedirectMiddleware extends LazyLogging {
  /**
    * Copies `org.http4s.server.middleware.HttpsRedirect`
    */
  def apply[F[_], G[_]](config: HttpServerConfig)(http: Http[F, G])(implicit F: Applicative[F]): Http[F, G] =
    if (!config.forceHTTPS) http else {
      Kleisli { req =>
        (HttpUtils.getForwardedProto(req), req.headers.get(Host)) match {
          case (Some(Scheme.http), Some(host)) =>
            logger.debug(s"Redirecting ${req.method} ${req.uri} to https on $host")
            val authority = Authority(host = RegName(host.value))
            val location = req.uri.copy(scheme = Some(Scheme.https), authority = Some(authority))
            val headers = Headers(Location(location) :: `Content-Type`(MediaType.text.xml) :: Nil)
            val response = Response[G](status = MovedPermanently, headers = headers)
            response.pure[F]
          case _ =>
            http(req)
        }
      }
    }
}
