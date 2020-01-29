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

package org.alexn.httpdope.utils

import cats.Applicative
import cats.data.Kleisli
import cats.implicits._
import org.alexn.httpdope.config.HttpServerConfig
import org.http4s.Status.MovedPermanently
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.http4s.headers.{Host, Location, `Content-Type`}
import org.http4s.{Headers, Http, MediaType, Response}
import org.log4s.getLogger

object CanonicalRedirectMiddleware {
  private val logger = getLogger

  def apply[F[_], G[_]](config: HttpServerConfig)(http: Http[F, G])(implicit F: Applicative[F]): Http[F, G] =
    if (!config.canonicalRedirect) http else {
      Kleisli { req =>
        req.headers.get(Host).map(_.value) match {
          case Some(host) if host != config.canonicalDomain.value =>
            val location = {
              val authority = Authority(host = RegName(config.canonicalDomain.value), port = None)
              val withDomain = req.uri.copy(authority = Some(authority))
              if (config.forceHTTPS) withDomain.copy(scheme = Some(Scheme.https)) else withDomain
            }

            logger.debug(s"Redirecting to ${location.renderString}")
            val headers = Headers(Location(location) :: `Content-Type`(MediaType.text.xml) :: Nil)
            val response = Response[G](status = MovedPermanently, headers = headers)
            response.pure[F]

          case _ =>
            http(req)
        }
      }
    }
}
