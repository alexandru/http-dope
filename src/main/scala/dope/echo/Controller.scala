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

package dope.echo

import cats.Parallel
import cats.effect.{Sync, Timer}
import cats.implicits._
import dope.common.http.{BaseController, HttpUtils}
import dope.common.models.{IP, IPType}
import dope.common.utils.SystemCommands
import dope.generated.BuildInfo
import org.http4s.{HttpRoutes, Request, Response}

import scala.collection.immutable.ListMap
import scala.concurrent.duration._
import org.http4s.EntityEncoder

final class Controller[F[_]: Sync: Parallel](geoIP: Option[MaxmindGeoIPService[F]], system: SystemCommands[F])
  (implicit timer: Timer[F])
  extends BaseController[F] {

  def routes: HttpRoutes[F] = {
    val cachePolicy = HttpUtils.cached[F](Duration.MinusInf) _

    HttpRoutes.of[F] {
      case request @ GET -> Root / "ip" =>
        getIP(request).map(cachePolicy)

      case request @ GET -> Root / "all" =>
        getAll(request).map(cachePolicy)

      case request @ GET -> Root / "geoip" =>
        getGeoIP(request).map(cachePolicy)

      case GET -> Root / "server" =>
        getServer.map(cachePolicy)

      case GET -> Root / "timeout" / Duration(d, unit)  =>
        val timespan = FiniteDuration(d, unit)
        simulateTimeout(timespan)
    }
  }

  def getAll(request: Request[F]): F[Response[F]] = {
    def buildInfo(
      request: Request[F],
      clientGeoIP: Option[GeoIPInfo],
    ): F[Response[F]] = {
      val headers = {
        val list = request.headers.toList.map { h => (h.name.value, h.value) }
        ListMap(list:_*)
      }

      Ok(RequestInfo(
        request = ParsedRequest(
          detectedIP = IPUtils.extractClientIP(request),
          forwardedFor = HttpUtils.getHeader(request, "X-Forwarded-For"),
          via = HttpUtils.getHeader(request, "Via"),
          agent = HttpUtils.getHeader(request, "User-Agent"),
          headers = headers,
        ),
        clientGeoIP = clientGeoIP
      ))
    }

    Sync[F].suspend {
      val clientIP = extractIPFromRequest(request)
      for {
        clientGeoIP <- clientIP.fold(geoIPEmptyResult)(findGeoIP)
        response    <- buildInfo(request, clientGeoIP)
      } yield {
        response
      }
    }
  }

  def getIP(request: Request[F]): F[Response[F]] =
    IPUtils.extractClientIP(request) match {
      case Some(ip) => Ok(ip.value)
      case None => NoContent()
    }

  def getGeoIP(request: Request[F]): F[Response[F]] = {
    Sync[F].suspend {
      val ip = extractIPFromRequest(request)
      ip.fold(Option.empty[GeoIPInfo].pure[F])(findGeoIP).flatMap {
        case None =>
          notFound("ip", ip)
        case Some(info) =>
          Ok(info)
      }
    }
  }

  def getServer: F[Response[F]] = {
    val ipv4 = system.getServerIP(IPType.V4).flatMap(findGeoIPTuple)
    val ipv6 = system.getServerIP(IPType.V6).flatMap(findGeoIPTuple)

    (ipv4, ipv6).parTupled.flatMap { case (ipv4, ipv6) =>
      Ok(ServerInfo(ipv4, ipv6, BuildInfo.version, system.startedAtRealTime))
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

  private def findGeoIPTuple(ip: Option[IP]): F[Option[ServerIPInfo]] =
    ip match {
      case None => Sync[F].pure(None)
      case Some(ip) => findGeoIP(ip).map(r => Some(ServerIPInfo(ip, r)))
    }

  private def findGeoIP(ip: IP): F[Option[GeoIPInfo]] = {
    geoIP match {
      case None => Sync[F].pure(None)
      case Some(s) => s.findIP(ip)
    }
  }

  private def geoIPEmptyResult: F[Option[GeoIPInfo]] = {
    Sync[F].pure(Option.empty[GeoIPInfo])
  }

  private def extractIPFromRequest(request: Request[F]): Option[IP] =
    request.params.get("ip") match {
      case Some(value) if IPUtils.isPublicIP(IP(value)) => Some(IP(value))
      case _ => IPUtils.extractClientIP(request)
    }
}

object Controller {
  /** Builder. */
  def apply[F[_] : Sync : Parallel](
    geoIP: Option[MaxmindGeoIPService[F]],
    system: SystemCommands[F])
    (implicit timer: Timer[F]): Controller[F] = {

    new Controller(geoIP, system)
  }
}
