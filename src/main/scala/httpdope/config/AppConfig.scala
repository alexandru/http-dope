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

import cats.implicits._
import cats.effect.Sync
import com.typesafe.config.{Config, ConfigException}
import httpdope.echo.{MaxmindEdition, MaxmindGeoIPConfig, MaxmindLicenceKey}
import httpdope.vimeo.{VimeoConfig, VimeoAccessToken}

final case class AppConfig(
  source: ConfigSource,
  httpServer: HttpServerConfig,
  maxmindGeoIP: MaxmindGeoIPConfig,
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
        maxmindGeoIP = MaxmindGeoIPConfig(
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
        ),
        vimeo = VimeoConfig(
          accessToken = getStringOption(config, "vimeo.accessToken").map(VimeoAccessToken.apply)
        )
      )
    }
  }
}
