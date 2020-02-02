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

package httpdope.vimeo

import io.circe.generic.JsonCodec

@JsonCodec
final case class VimeoConfigJSON(
  uri: String,
  name: String,
  description: Option[String],
  link: String,
  duration: Option[Long],
  width: Option[Int],
  height: Option[Int],
  pictures: VimeoPicturesJSON
)

@JsonCodec
final case class VimeoPicturesJSON(
  uri: String,
  active: Boolean,
  `type`: Option[String],
  sizes: List[PicturesEntrySizeJSON],
  resource_key: Option[String]
)

@JsonCodec
final case class PicturesEntrySizeJSON(
  width: Int,
  height: Int,
  link: String,
  link_with_play_button: Option[String]
)