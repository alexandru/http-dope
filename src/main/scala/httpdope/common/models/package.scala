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

package httpdope.common

import httpdope.common.utils.JSONFormatters
import httpdope.echo.IPUtils
import io.estatico.newtype.macros.newtype

package object models {
  @newtype case class IP(value: String) {
    /**
      * Returns `true` if this is a valid public IP, `false` otherwise.
      */
    def isPublicIP: Boolean =
      IPUtils.isPublicIP(this)
  }

  object IP {
    implicit val jsonFormat =
      new JSONFormatters.Derived[String, IP](IP(_), _.value)
  }
}
