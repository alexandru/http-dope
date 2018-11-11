/*
 * Copyright (c) 2018 Alexandru Nedelcu.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.alexn.vdp

import cats.effect.ExitCode
import com.twitter.finagle.Http
import com.twitter.util.Time
import io.finch._
import monix.eval.{Task, TaskApp}
import org.alexn.vdp.utils.{Logger, MonixIntegration}

object Main extends TaskApp with MonixIntegration with Logger {

  def helloWorld: Endpoint[Task, String] = get("hello") {
    Ok("Hello, World!")
  }

  def hello: Endpoint[Task, String] = get("hello" :: path[String]) { s: String =>
    Ok("Hello, " + s + "!")
  }

  def startServer(address: String): Task[Nothing] =
    Task.cancelable0 { (_, _) =>
      val server = Http
        .server
        .serve(":8081", (hello :+: helloWorld).toServiceAs[Text.Plain])

      logger.info(s"Listening on $address")
      Task {
        logger.warn("Stopping server")
        server.close(Time.Top)
      }
    }

  def run(args: List[String]): Task[ExitCode] =
    for {
      _ <- startServer(":8081")
      _ <- Task(logger.info("Started server at: $"))
    } yield {
      ExitCode.Success
    }
}
