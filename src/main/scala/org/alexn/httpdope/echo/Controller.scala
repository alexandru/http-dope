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

import cats.effect.{Sync, Timer}
import cats.implicits._
import org.alexn.httpdope.utils.{BaseController, RequestUtils}
import org.http4s.{HttpRoutes, Request, Response}
import scala.concurrent.duration._

final class Controller[F[_]](geoIP: MaxmindGeoIPService[F])
  (implicit F: Sync[F], timer: Timer[F])
  extends BaseController[F] {

  def routes =
    HttpRoutes.of[F] {
      case request @ GET -> Root / "ip" =>
        getIP(request)

      case request @ GET -> Root / "all" =>
        getAll(request)

      case request @ GET -> Root / "ip" =>
        getIP(request)

      case request @ GET -> Root / "geoip" =>
        getGeoIP(request)

      case GET -> Root / "timeout" / Duration(d, unit)  =>
        val timespan = FiniteDuration(d, unit)
        simulateTimeout(timespan)
    }

  def getAll(request: Request[F]): F[Response[F]] = {
    F.suspend {
      val ip = extractIPFromRequest(request)
      ip.fold(F.pure(Option.empty[GeoIPInfo]))(geoIP.findIP).flatMap { ipInfo =>

        val headers = request.headers.toList.map { h =>
          (h.name.value, h.value)
        }

        Ok(RequestInfo(
          request = ParsedRequest(
            detectedIP = IPUtils.extractClientIP(request).map(IP(_)),
            forwardedFor = RequestUtils.getHeader(request, "X-Forwarded-For"),
            via = RequestUtils.getHeader(request, "Via"),
            agent = RequestUtils.getHeader(request, "User-Agent"),
            headers = headers
          ),
          geoip = ipInfo
        ))
      }
    }
  }

  def getIP(request: Request[F]): F[Response[F]] =
    IPUtils.extractClientIP(request) match {
      case Some(ip) => Ok(ip)
      case None => NoContent()
    }

  def getGeoIP(request: Request[F]): F[Response[F]] = {
    F.suspend {
      val ip = extractIPFromRequest(request)
      ip.fold(F.pure(Option.empty[GeoIPInfo]))(geoIP.findIP).flatMap {
        case None =>
          notFound("ip", ip)
        case Some(info) =>
          Ok(info)
      }
    }
  }

  def simulateTimeout(ts: FiniteDuration): F[Response[F]] = {
    for {
      start <- timer.clock.monotonic(MILLISECONDS)
      _ <- timer.sleep(ts)
      now <- timer.clock.monotonic(MILLISECONDS)
      r <- timeout("sleptMillis", Some(now - start))
    } yield r
  }

  private def extractIPFromRequest(request: Request[F]) =
    request.params.get("ip") match {
      case ip@Some(value) if IPUtils.isPublicIP(value) => ip
      case _ => IPUtils.extractClientIP(request)
    }
}

object Controller {
  /** Builder. */
  def apply[F[_]](geoIP: MaxmindGeoIPService[F])(implicit F: Sync[F], timer: Timer[F]): Controller[F] =
    new Controller(geoIP)
}
