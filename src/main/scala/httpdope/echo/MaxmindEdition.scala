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

package httpdope.echo

sealed abstract class MaxmindEdition(val id: String)

object MaxmindEdition {
  def apply(id: String): Option[MaxmindEdition] =
    id match {
      case GeoLite2City.id => Some(GeoLite2City)
      case GeoLite2Country.id => Some(GeoLite2Country)
      case _ => None
    }

  final case object GeoLite2City extends MaxmindEdition("GeoLite2-City")
  final case object GeoLite2Country extends MaxmindEdition("GeoLite2-Country")
}
