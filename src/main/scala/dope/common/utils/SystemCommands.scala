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

package dope.common.utils

import java.io.{BufferedReader, InputStreamReader}
import java.time.{Instant, OffsetDateTime, ZoneId}
import java.util.concurrent.TimeUnit
import cats.effect.{Blocker, Clock, Concurrent, ContextShift, Resource, Sync}
import cats.implicits._
import dope.common.models.{IP, IPType}
import dope.common.utils.CacheManager.Cache
import scala.util.control.NonFatal

final case class SystemCommandsConfig(
  cache: CacheEvictionPolicy
)

final class SystemCommands[F[_]] private (
  val startedAtRealTime: OffsetDateTime,
  cache: Cache[F, String, Option[String]],
  blocker: Blocker
)(implicit
  F: Sync[F],
  cs: ContextShift[F],
) extends StrictLogging {

  def getServerIP(kind: IPType): F[Option[IP]] =
    kind match {
      case IPType.V4 => getServerIPv4
      case IPType.V6 => getServerIPv6
    }

  private val getServerIPv4: F[Option[IP]] = {
    executeCommand("dig -4 +short myip.opendns.com @resolver1.opendns.com")
      .map(_.map(IP(_)))
  }

  private val getServerIPv6: F[Option[IP]] = {
    executeCommand("dig -6 TXT +short o-o.myaddr.l.google.com @ns1.google.com")
      .map(_.map(s => IP(s.replaceAll("\"", ""))))
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
    (implicit F: Concurrent[F], cs: ContextShift[F], clock: Clock[F]): Resource[F, SystemCommands[F]] = {

    for {
      cache <- cacheManager.createCache[String, Option[String]]("SystemCommands", config.cache)
      startReal <- Resource.liftF(clock.realTime(TimeUnit.MILLISECONDS))
    } yield {
      val dt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(startReal), ZoneId.of("UTC"))
      new SystemCommands[F](dt, cache, blocker)
    }
  }
}
