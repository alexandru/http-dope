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
package echo

import cats.effect.Resource
import httpdope.common.models.IP
import monix.eval.Task
import httpdope.config.AppConfig

class MaxmindGeoIPServiceSuite extends AsyncBaseSuite.OfTask {

  testEffect("simple query from fresh database") {
    Task.suspend {
      val isCI = Option(System.getenv("CI")).fold(false)(_.toLowerCase.trim == "true")
      assume(isCI, "environment is not CI")
      testQuery(refreshDBOnRun = true)
    }
  }

  testEffect("simple query from local database") {
    testQuery(refreshDBOnRun = false)
  }

  def testQuery(refreshDBOnRun: Boolean) = {
    val service = for {
      cfg <- Resource.liftF(AppConfig.loadFromEnv)
      ref <- MaxmindGeoIPService.test[Task](Some(cfg.copy(maxmindGeoIP = cfg.maxmindGeoIP.map(_.copy(refreshDBOnRun = refreshDBOnRun)))))
    } yield {
      ref
    }

    service.use { service =>
      for {
        info <- service.findIP(IP("109.101.232.97"))
      } yield {
        assert(info.flatMap(_.country) === Some(GeoIPCountry("RO",Some("Romania"), isInEuropeanUnion = true)))
        assert(info.flatMap(_.continent) === Some(GeoIPContinent("EU", Some("Europe"))))
      }
    }
  }
}
