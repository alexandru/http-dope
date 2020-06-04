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

package dope.echo

import java.time.OffsetDateTime

import dope.common.models.IP
import io.circe.generic.JsonCodec

@JsonCodec
final case class RequestInfo(
  request: ParsedRequest,
  server: ServerInfo,
  clientGeoIP: Option[GeoIPInfo],
)

@JsonCodec
final case class ParsedRequest(
  detectedIP: Option[IP],
  forwardedFor: Option[String],
  via: Option[String],
  agent: Option[String],
  headers: Map[String, String],
)

@JsonCodec
final case class ServerInfo(
  ipv4: Option[ServerIPInfo],
  ipv6: Option[ServerIPInfo],
  buildVersion: String,
  startedAt: OffsetDateTime
)

@JsonCodec
final case class ServerIPInfo(
  value: IP,
  geoInfo: Option[GeoIPInfo],
)
