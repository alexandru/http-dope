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
package config

import monix.eval.Task
import org.alexn.httpdope.echo._

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
            edition = MaxmindEdition.GeoLite2Country
          )
        ))
      }
    }
  }

  testEffect("loads config (application.default.conf)") {
    withSystemProperty("config.resource", Some("application.test-defaults.conf")).use { _ =>
      for {
        config <- AppConfig.loadFromEnv[Task]
      } yield {
        assert(config === AppConfig(
          ConfigSource.Resource("application.test-defaults.conf"),
          httpServer = HttpServerConfig(
            address = HTTPAddress("0.0.0.0"),
            port = HTTPPort(8080),
            canonicalDomain = DomainName("dope.alexn.org"),
            canonicalRedirect = false,
            forceHTTPS = true
          ),
          maxmindGeoIP = MaxmindGeoIPConfig(
            apiKey = MaxmindLicenceKey("my-api-key"),
            edition = MaxmindEdition.GeoLite2City
          )
        ))
      }
    }
  }
}
