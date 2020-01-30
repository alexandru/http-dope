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

import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, Timer}
import cats.implicits._
import fs2.Stream
import monix.eval._
import monix.execution.Scheduler
import org.alexn.httpdope.config.AppConfig
import org.alexn.httpdope.utils._
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{AutoSlash, Logger}

object Server {

  def stream[F[_]](implicit F: ConcurrentEffect[F], T: Timer[F], C: ContextShift[F], global: Scheduler): Stream[F, Nothing] = {
    for {
      config <- Stream.eval(AppConfig.loadFromEnv[F])
      (_, blocker) <- Stream.resource(Schedulers.createBlockingContext())
      _ <- BlazeClientBuilder[F](global).stream

      allRoutes = (
        static.Routes[F](blocker).staticRoutes
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
  def run(args: List[String]) = {
    implicit val ec = scheduler
    Server.stream[Task].compile.drain.as(ExitCode.Success)
  }
}
