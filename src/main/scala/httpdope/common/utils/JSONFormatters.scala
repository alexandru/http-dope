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

package httpdope.common.utils

import io.circe.Decoder.Result
import io.circe.{Decoder, DecodingFailure, Encoder, HCursor, Json}

object JSONFormatters {
  /**
    * Utility for deriving a JSON encoder/decoder from
    * validated String values.
    */
  final class JSONFormatFromValidatedString[A](build: String => Option[A], extract: A => String, error: String)
    extends Encoder[A] with Decoder[A] {

    def apply(o: A): Json =
      Encoder.encodeString(extract(o))

    def apply(c: HCursor): Result[A] =
      Decoder.decodeString(c).flatMap { id =>
        build(id) match {
          case None => Left(DecodingFailure(error, c.history))
          case Some(ref) => Right(ref)
        }
      }
  }

  /**
    * Utility for deriving a JSON encoder/decoder from
    * simple String values.
    */
  final class Derived[A, B](build: A => B, extract: B => A)(
    implicit srcEncoder: Encoder[A],
    srcDecoder: Decoder[A])
    extends Encoder[B] with Decoder[B] {

    def apply(o: B): Json =
      srcEncoder(extract(o))

    def apply(c: HCursor): Result[B] =
      srcDecoder(c).map(build)
  }
}
