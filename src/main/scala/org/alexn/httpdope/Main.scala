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

package org.alexn.httpdope

import cats.effect.ExitCode
import cats.implicits._
import monix.eval._
import cats.effect.{ConcurrentEffect, Timer}
import fs2.Stream
import org.alexn.httpdope.config.AppConfig
import org.alexn.httpdope.utils._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{AutoSlash, Logger}

object Server {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F]): Stream[F, Nothing] = {
    for {
      config <- Stream.eval(AppConfig.loadFromEnv[F])
      //httpClient <- BlazeClientBuilder[F](global).stream

      allRoutes = (
        echo.Routes[F].staticRoutes
      )

      // With all middleware
      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(
        CanonicalRedirectMiddleware(config.httpServer)(
          HttpsRedirect(config.httpServer)(
            AutoSlash(
              CORSMiddleware(
                allRoutes
              ))))
          .orNotFound
      )

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(config.httpServer.port.value, config.httpServer.address.value)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
    }.drain
}

object Main extends TaskApp {
  def run(args: List[String]) =
    Server.stream[Task].compile.drain.as(ExitCode.Success)
}
