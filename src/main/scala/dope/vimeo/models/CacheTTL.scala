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

package dope.vimeo.models

/**
  * Specifies for how long items should be cached,
  * used to discriminate on cache instances, as we can't set a
  * per-key expiry.
  */
sealed abstract class CacheTTL(val id: String)

object CacheTTL {
  final case object LongTerm extends CacheTTL("long")
  final case object ShortTerm extends CacheTTL("short")
  final case object NoCache extends CacheTTL("no-cache")
}
