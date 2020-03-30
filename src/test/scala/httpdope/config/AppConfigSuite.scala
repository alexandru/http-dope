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

import httpdope.common.utils.{CacheEvictionPolicy, SystemCommandsConfig}
import monix.eval.Task
import httpdope.echo._
import httpdope.vimeo.models.{VimeoAccessToken, VimeoCacheEvictionPolicy, VimeoConfig}

import scala.concurrent.duration._

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
          systemCommands = SystemCommandsConfig(
            cache = CacheEvictionPolicy(
              heapItems = 200,
              offHeapMB = Some(10),
              timeToLiveExpiration = Some(2.hours)
            )
          ),
          vimeo = VimeoConfig(
            accessToken = Some(VimeoAccessToken("my-access-token")),
            cache = VimeoCacheEvictionPolicy(
              shortTerm = CacheEvictionPolicy(
                heapItems = 200,
                offHeapMB = Some(10),
                timeToLiveExpiration = Some(10.minutes)
              ),
              longTerm = CacheEvictionPolicy(
                heapItems = 300,
                offHeapMB = Some(20),
                timeToLiveExpiration = Some(10.hours)
              )
            )
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
          systemCommands = SystemCommandsConfig(
            cache = CacheEvictionPolicy(
              heapItems = 100,
              offHeapMB = None,
              timeToLiveExpiration = Some(1.hour)
            )
          ),
          vimeo = VimeoConfig(
            accessToken = None,
            cache = VimeoCacheEvictionPolicy(
              shortTerm = CacheEvictionPolicy(
                heapItems = 100,
                offHeapMB = None,
                timeToLiveExpiration = Some(1.hour)
              ),
              longTerm = CacheEvictionPolicy(
                heapItems = 100,
                offHeapMB = Some(1),
                timeToLiveExpiration = Some(24.hours)
              )
            )
          )
        ))
      }
    }
  }
}
