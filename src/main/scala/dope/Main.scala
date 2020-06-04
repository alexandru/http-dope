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

package dope

import cats.NonEmptyParallel
import cats.effect.{Clock, ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.implicits._
import dope.common.http.{CORSMiddleware, CanonicalRedirectMiddleware, HttpsRedirectMiddleware}
import dope.common.utils._
import dope.config.AppConfig
import dope.static.Controller
import fs2.Stream
import monix.eval._
import monix.execution.Scheduler
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{AutoSlash, Logger}

object Server extends LazyLogging {

  def stream[F[_]](implicit F: ConcurrentEffect[F], PF: NonEmptyParallel[F], timer: Timer[F], cs: ContextShift[F], global: Scheduler): Stream[F, Nothing] = {
    implicit val clock: Clock[F] = timer.clock

    val stream = for {
      config <- Stream.eval(AppConfig.loadFromEnv[F])
      (_, blocker) <- Stream.resource(Schedulers.createBlockingContext())
      blazeClient <- BlazeClientBuilder[F](global).stream
      httpClient = HTTPClient(blazeClient, blocker)
      geoIP <- config.maxmindGeoIP match {
        case None =>
          Stream.eval(F.pure(None))
        case Some(cfg) =>
          Stream.resource(echo.MaxmindGeoIPService(cfg, httpClient, blocker).map(Some(_)))
      }
      cacheManager <- Stream.resource(CacheManager[F])
      system <- Stream.resource(SystemCommands[F](config.systemCommands, cacheManager, blocker))
      vimeoController <- Stream.resource(vimeo.Controller[F](config.vimeo, cacheManager, blazeClient, system))

      allRoutes = Router(
        "/" -> Controller[F](config.httpServer, blocker).routes,
        "/echo" -> echo.Controller[F](geoIP, system).routes,
        "/vimeo" -> vimeoController.routes,
      )

      // With all middleware
      finalHttpApp = Logger.httpApp(logHeaders = false, logBody = false)(
        CanonicalRedirectMiddleware(config.httpServer)(
          HttpsRedirectMiddleware(config.httpServer)(
            AutoSlash(
              CORSMiddleware(
                allRoutes
              ))))
          .orNotFound
      )

      _ <- Stream.eval(F.delay {
        logger.info(s"Starting server at ${config.httpServer.address.value}:${config.httpServer.port.value}")
      })
      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(config.httpServer.port.value, config.httpServer.address.value)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode

    stream.drain
  }
}

object Main extends TaskApp {
  def run(args: List[String]) = {
    implicit val ec = scheduler
    Server.stream[Task].compile.drain.as(ExitCode.Success)
  }
}
