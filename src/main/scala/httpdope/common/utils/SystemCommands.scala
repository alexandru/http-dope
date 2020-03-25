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

package httpdope.common.utils

import java.io.{BufferedReader, InputStreamReader}
import cats.effect.{Blocker, Clock, Concurrent, ContextShift, Sync}
import cats.implicits._
import httpdope.common.models.IP
import httpdope.common.utils.CacheManager.Cache
import scala.util.control.NonFatal

final case class SystemCommandsConfig(
  cache: CacheEvictionPolicy
)

final class SystemCommands[F[_]] private (cache: Cache[F, String, Option[String]], blocker: Blocker)
  (implicit F: Sync[F], cs: ContextShift[F])
  extends StrictLogging {

  /**
    * Task for getting the server's IP.
    */
  val getServerIP: F[Option[IP]] = {
    executeCommand("dig +short myip.opendns.com @resolver1.opendns.com")
      .map(_.map(IP(_)))
  }

  /**
    * Execute a system command and caches it in internal memory.
    */
  def executeCommand(cmd: String): F[Option[String]] =
    cache.getOrEvalIfAbsent(cmd, executeCommandUncached(cmd))

  /**
    * Execute a system command and returns the result.
    */
  def executeCommandUncached(cmd: String): F[Option[String]] =
    blocker.blockOn(F.delay {
      try {
        val p = Runtime.getRuntime.exec(cmd)
        if (p.waitFor() != 0)
          throw new RuntimeException("Process exited in error!")

        val in = new BufferedReader(new InputStreamReader(p.getInputStream))
        val sb = new StringBuilder()

        var line = ""
        while (line != null) {
          line = in.readLine()
          if (line != null) sb.append(line)
        }
        Some(sb.toString())
      } catch {
        case NonFatal(e) =>
          logger.error(e)(s"Unexpected error while executing: $cmd")
          None
      }
    })
}

object SystemCommands {
  def apply[F[_]](
    config: SystemCommandsConfig,
    cacheManager: CacheManager[F],
    blocker: Blocker)
    (implicit F: Concurrent[F], cs: ContextShift[F]): F[SystemCommands[F]] = {

    for {
      cache <- cacheManager.createCache[String, Option[String]]("SystemCommands", config.cache)
    } yield {
      new SystemCommands[F](cache, blocker)
    }
  }
}
