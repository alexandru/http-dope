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

package httpdope.config

import java.util.concurrent.TimeUnit

import cats.implicits._
import cats.effect.Sync
import com.typesafe.config.{Config, ConfigException}
import httpdope.common.utils.{CacheEvictionPolicy, SystemCommandsConfig}
import httpdope.echo.{MaxmindEdition, MaxmindGeoIPConfig, MaxmindLicenceKey}
import httpdope.vimeo.models.{VimeoAccessToken, VimeoCacheEvictionPolicy, VimeoConfig}

import scala.concurrent.duration._

final case class AppConfig(
  source: ConfigSource,
  httpServer: HttpServerConfig,
  maxmindGeoIP: Option[MaxmindGeoIPConfig],
  systemCommands: SystemCommandsConfig,
  vimeo: VimeoConfig
)

/**
  * General server settings.
  */
final case class HttpServerConfig(
  address: HTTPAddress,
  port: HTTPPort,
  canonicalDomain: DomainName,
  canonicalRedirect: Boolean,
  forceHTTPS: Boolean
)

object AppConfig {
  /**
    * Builds an [[AppConfig]] reference by parsing the file path
    * indicated by environment values.
    */
  def loadFromEnv[F[_]: Sync]: F[AppConfig] =
    ConfigUtil.loadFromEnv[F].flatMap {
      case (source, config) =>
        load(source, config)
    }

  /**
    * Parses a `Config` object into an [[AppConfig]].
    */
  def load[F[_]: Sync](source: ConfigSource, config: Config): F[AppConfig] = {
    def getStringOption[A](config: Config, key: String): Option[String] =
      if (config.hasPath(key)) Option(config.getString(key)) else None

    def getLongOption[A](config: Config, key: String): Option[Long] =
      if (config.hasPath(key)) Option(config.getLong(key)) else None

    def getDurationOption[A](config: Config, key: String): Option[FiniteDuration] =
      if (config.hasPath(key)) Option(config.getDuration(key, TimeUnit.MILLISECONDS).millis) else None

    def getCachePolicy(config: Config, prefix: String): CacheEvictionPolicy =
      CacheEvictionPolicy(
        heapItems = config.getLong(s"$prefix.heapItems"),
        offHeapMB = getLongOption(config, s"$prefix.offHeapMB"),
        timeToLiveExpiration = getDurationOption(config, s"$prefix.timeToLiveExpiration")
      )

    Sync[F].delay {
      AppConfig(
        source,
        httpServer = HttpServerConfig(
          address = HTTPAddress(config.getString("http.server.address")),
          port = HTTPPort(config.getInt("http.server.port")),
          canonicalDomain = DomainName(config.getString("http.server.canonicalDomain").toLowerCase),
          canonicalRedirect = config.getBoolean("http.server.canonicalRedirect"),
          forceHTTPS = config.getBoolean("http.server.forceHTTPS")
        ),
        maxmindGeoIP = {
          if (config.hasPath("maxmindGeoIP.isEnabled") && config.getBoolean("maxmindGeoIP.isEnabled") && config.hasPath("maxmindGeoIP.apiKey"))
            Some(MaxmindGeoIPConfig(
              apiKey = MaxmindLicenceKey(config.getString("maxmindGeoIP.apiKey")),
              refreshDBOnRun = config.getBoolean("maxmindGeoIP.refreshDBOnRun"),
              edition = {
                val key = "maxmindGeoIP.edition"
                val value = config.getString(key)
                MaxmindEdition(value)
                  .getOrElse(throw new ConfigException.WrongType(
                    config.getValue(key).origin(),
                    s"Unexpected MaxmindEdition ID: $value"
                  ))
              }
            ))
          else
            None
        },
        vimeo = VimeoConfig(
          accessToken = getStringOption(config, "vimeo.accessToken").map(VimeoAccessToken.apply),
          cache = VimeoCacheEvictionPolicy(
            longTerm = getCachePolicy(config, "vimeo.cache.longTerm"),
            shortTerm = getCachePolicy(config, "vimeo.cache.shortTerm"),
          )
        ),
        systemCommands = SystemCommandsConfig(
          cache = getCachePolicy(config, "systemCommands.cache"),
        )
      )
    }
  }
}
