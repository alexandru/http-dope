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

package httpdope.common.models

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets

import io.circe.generic.JsonCodec

/**
  * Models errors that can be thrown by web services:
  *
  *   - HTTP code errors ([[HttpError]])
  *   - JSON parsing errors ([[JSONError]])
  *   - ...
  */
@JsonCodec
sealed trait WebError

@JsonCodec
final case class HttpError(status: Int, body: String, contentType: Option[String])
  extends WebError

@JsonCodec
final case class JSONError(message: String)
  extends WebError

@JsonCodec
final case class UncaughtException(cls: String, message: String, trace: String)
  extends WebError

object UncaughtException {
  /**
    * Builds a value from a plain exception.
    */
  def apply(e: Throwable): UncaughtException = {
    val bs = new ByteArrayOutputStream
    val ps = new PrintStream(bs, true, "UTF-8")
    e.printStackTrace(ps)
    val trace = new String(bs.toByteArray, StandardCharsets.UTF_8)
    UncaughtException(e.getClass.getName, e.getMessage, trace)
  }
}
