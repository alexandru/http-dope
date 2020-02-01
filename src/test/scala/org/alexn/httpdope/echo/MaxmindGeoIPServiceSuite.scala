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
package echo

import cats.effect.Resource
import monix.eval.Task
import org.alexn.httpdope.config.AppConfig

class MaxmindGeoIPServiceSuite extends AsyncBaseSuite.OfTask {

  testEffect("simple query from fresh database") {
    Task.suspend {
      // val isCI = Option(System.getenv("CI")).fold(false)(_.toLowerCase.trim == "true")
      // assume(isCI, "environment is not CI")
      testQuery(refreshDBOnRun = true)
    }
  }

  testEffect("simple query from local database") {
    testQuery(refreshDBOnRun = false)
  }

  def testQuery(refreshDBOnRun: Boolean) = {
    val service = for {
      cfg <- Resource.liftF(AppConfig.loadFromEnv)
      ref <- MaxmindGeoIPService.test[Task](Some(cfg.copy(maxmindGeoIP = cfg.maxmindGeoIP.copy(refreshDBOnRun = refreshDBOnRun))))
    } yield {
      ref
    }

    service.use { service =>
      for {
        info <- service.findIP("185.216.34.232")
      } yield {
        assert(info === Some(
          GeoIPInfo(
            IP("185.216.34.232"),
            Some(GeoIPCountry("AT",Some("Austria"), isInEuropeanUnion = true)),
            Some(GeoIPCountry("AT", Some("Austria"), isInEuropeanUnion = true)),
            None,
            Some(GeoIPContinent("EU", Some("Europe"))),
            Some(GeoIPCity("Vienna")),
            Some(GeoIPLocation(48.2994,16.3479,Some("Europe/Vienna"),Some(200))),
            Some(GeoIPPostalCode("1210")),
          )))
      }
    }
  }
}
