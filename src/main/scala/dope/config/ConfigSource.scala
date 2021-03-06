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

package dope.config

/**
  * Indicates the source from where the Typesafe Config must be loaded.
  */
sealed trait ConfigSource

object ConfigSource {

  /**
    * Represents a resource name that could be loaded via:
    *
    * `getClass.getResourceAsStream(name)`
    */
  final case class Resource(name: String) extends ConfigSource

  /**
    * Represents a file path.
    */
  final case class Path(name: String) extends ConfigSource

  /**
    * A config source that's neither a known path or resource name,
    * usually used for manually built configuration objects
    * (not loaded via [[ConfigUtil.loadFromEnv]]).
    *
    * This happens for example when the configuration is injected
    * by the Play Framework.
    */
  final case object Injected extends ConfigSource
}
