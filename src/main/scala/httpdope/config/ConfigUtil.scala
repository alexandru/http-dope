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

package httpdope.config

import cats.implicits._
import java.io.File
import cats.effect.Sync
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Utilities for reading the application's global configuration file.
  *
  * The configuration file is using HOCON, a JSON superset, see:
  * [[https://github.com/lightbend/config/blob/master/HOCON.md]]
  */
object ConfigUtil {

  /**
    * Extracts a [[ConfigSource]] from system properties or environment
    * variables, like so:
    *
    *  - if a config file is set via a system property `config.file`,
    *    specifying a file path, then that file path is returned
    *  - if an environment name is specified via an environment variable
    *    called `ENV`, then a configuration file is indicated as a resource,
    *    named `application.{ENV}.conf`
    *  - if no system properties or environment values are detected,
    *    then a resource named `application.conf` is returned
    *
    * You can specify a system property via the `java` command like so:
    *
    * `java -Dconfig.file=/etc/fama/application.conf`
    *
    * Or you can set an environment variable called `ENV`, which will
    * trigger the loading of an environment-specific resource:
    *
    * `ENV=production java ...`
    */
  def getConfigSource[F[_]: Sync]: F[ConfigSource] =
    Sync[F].delay {
      Option(System.getProperty("config.file")) match {
        case Some(path) if new File(path).exists() =>
          ConfigSource.Path(path)

        case _ =>
          Option(System.getProperty("config.resource")) match {
            case Some(res) =>
              ConfigSource.Resource(res)

            case None =>
              val opt1 = Option(System.getenv("ENV")).filter(_.nonEmpty)
              val opt2 = Option(System.getenv("env")).filter(_.nonEmpty)

              opt1.orElse(opt2) match {
                case Some(envName) =>
                  val name = s"application.${envName.toLowerCase}.conf"
                  ConfigSource.Resource(name)
                case None =>
                  ConfigSource.Resource("application.conf")
              }
          }
      }
    }

  /**
    * Loads the global HOCON configuration file based on the given
    * [[ConfigSource]].
    */
  def load[F[_]: Sync](source: ConfigSource): F[Config] =
    Sync[F].delay {
      val default = ConfigFactory.load("application.default.conf").resolve()

      // Loads the configuration file depending on the source
      val config = source match {
        case ConfigSource.Path(path) =>
          ConfigFactory.parseFile(new File(path)).resolve()
        case ConfigSource.Resource(name) =>
          ConfigFactory.load(name).resolve()
        case ConfigSource.Injected =>
          ConfigFactory.load().resolve()
      }

      // Specifies the default values
      config.withFallback(default)
    }

  /**
    * Loads the global HOCON configuration file based on a [[ConfigSource]]
    * specified by system properties, or environment variables, as extracted
    * by [[getConfigSource]].
    *
    * @see the documentation of [[getConfigSource]] to see the rules for
    *      finding and loading a configuration file
    */
  def loadFromEnv[F[_]: Sync]: F[(ConfigSource, Config)] =
    for {
      source <- getConfigSource[F]
      config <- load[F](source)
    } yield (source, config)
}
