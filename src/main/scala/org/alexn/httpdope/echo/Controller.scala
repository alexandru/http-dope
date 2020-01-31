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

package org.alexn.httpdope.echo

import cats.effect.Sync
import com.github.ghik.silencer.silent
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Request, Response}

final class Controller[F[_]](@silent geoIP: MaxmindGeoIPService[F])(implicit F: Sync[F]) {
  val dsl = new Http4sDsl[F] {}
  import dsl._

  def routes =
    HttpRoutes.of[F] {
      case request @ GET -> Root / "ip" =>
        getIP(request)
    }

  def getIP(request: Request[F]): F[Response[F]] =
    IPUtils.extractClientIP(request) match {
      case Some(ip) => Ok(ip)
      case None => NoContent()
    }
}

object Controller {
  /** Builder. */
  def apply[F[_]](geoIP: MaxmindGeoIPService[F])(implicit F: Sync[F]): Controller[F] =
    new Controller(geoIP)
}
