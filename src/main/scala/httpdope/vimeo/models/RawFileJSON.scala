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

package httpdope.vimeo.models

import io.circe.generic.JsonCodec

@JsonCodec
final case class RawFileJSON(
  width: Option[Int],
  height: Option[Int],
  size: Option[String],
  public_name: String,
  extension: String,
  download_name: String,
  download_url: String,
  is_cold: Option[Boolean],
  is_defrosting: Option[Boolean],
  range: Option[String]
)
