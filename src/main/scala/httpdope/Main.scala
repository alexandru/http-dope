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

package httpdope

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.implicits._
import fs2.Stream
import monix.eval._
import monix.execution.Scheduler
import httpdope.config.AppConfig
import httpdope.echo.MaxmindGeoIPService
import httpdope.common.http._
import httpdope.common.utils._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{AutoSlash, Logger}

object Server extends LazyLogging {

  def stream[F[_]](implicit F: ConcurrentEffect[F], timer: Timer[F], cs: ContextShift[F], global: Scheduler): Stream[F, Nothing] = {
    val stream = for {
      config <- Stream.eval(AppConfig.loadFromEnv[F])
      (_, blocker) <- Stream.resource(Schedulers.createBlockingContext())
      blazeClient <- BlazeClientBuilder[F](global).stream
      httpClient = HTTPClient(blazeClient, blocker)
      geoIP <- Stream.resource(MaxmindGeoIPService(config.maxmindGeoIP, httpClient, blocker))
      cacheManager <- Stream.resource(CacheManager[F])
      system <- Stream.resource(SystemCommands[F](config.systemCommands, cacheManager, blocker))
      vimeoController <- Stream.resource(vimeo.Controller[F](config.vimeo, cacheManager, blazeClient, system))

      allRoutes = Router(
        "/" -> static.Controller[F](config.httpServer, blocker).routes,
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
      exitCode <- BlazeServerBuilder[F]
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