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

import io.circe.generic.JsonCodec

@JsonCodec
final case class GeoIPCountry(
  isoCode: String,
  name: Option[String],
  isInEuropeanUnion: Boolean
)

@JsonCodec
final case class GeoIPCity(
  name: String
)

@JsonCodec
final case class GeoIPLocation(
  latitude: Double,
  longitude: Double,
  timezone: Option[String],
  accuracyRadius: Option[Int]
)

@JsonCodec
final case class GeoIPPostalCode(
  code: String
)

@JsonCodec
final case class GeoIPContinent(
  code: String,
  name: Option[String]
)

@JsonCodec
final case class GeoIPInfo(
  ip: IP,
  country: Option[GeoIPCountry],
  registeredCountry: Option[GeoIPCountry],
  representedCountry: Option[GeoIPCountry],
  continent: Option[GeoIPContinent],
  city: Option[GeoIPCity],
  location: Option[GeoIPLocation],
  postal: Option[GeoIPPostalCode],
)
