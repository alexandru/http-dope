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
package config

import monix.eval.Task
import httpdope.echo._
import httpdope.vimeo.models.{VimeoAccessToken, VimeoConfig}

class AppConfigSuite extends AsyncBaseSuite.OfTask {
  testEffect("loads config (application.test.conf)") {
    withSystemProperty("config.resource", Some("application.test.conf")).use { _ =>
      for {
        config <- AppConfig.loadFromEnv[Task]
      } yield {
        assert(config === AppConfig(
          ConfigSource.Resource("application.test.conf"),
          httpServer = HttpServerConfig(
            address = HTTPAddress("127.0.0.1"),
            port = HTTPPort(10000),
            canonicalDomain = DomainName("test.org"),
            canonicalRedirect = true,
            forceHTTPS = false
          ),
          maxmindGeoIP = MaxmindGeoIPConfig(
            apiKey = MaxmindLicenceKey("test-api-key"),
            edition = MaxmindEdition.GeoLite2Country,
            refreshDBOnRun = true
          ),
          vimeo = VimeoConfig(
            accessToken = Some(VimeoAccessToken("my-access-token"))
          )
        ))
      }
    }
  }

  testEffect("loads config (application.default.conf)") {
    withSystemProperty("config.resource", Some("application.conf")).use { _ =>
      for {
        config <- AppConfig.loadFromEnv[Task]
      } yield {
        assert(config === AppConfig(
          ConfigSource.Resource("application.conf"),
          httpServer = HttpServerConfig(
            address = HTTPAddress("0.0.0.0"),
            port = HTTPPort(8080),
            canonicalDomain = DomainName("dope.alexn.org"),
            canonicalRedirect = false,
            forceHTTPS = true
          ),
          maxmindGeoIP = MaxmindGeoIPConfig(
            apiKey = MaxmindLicenceKey(System.getenv("DOPE_MAXMIND_GEOIP_API_KEY")),
            edition = MaxmindEdition.GeoLite2City,
            refreshDBOnRun = false
          ),
          vimeo = VimeoConfig(
            accessToken = None
          )
        ))
      }
    }
  }
}
