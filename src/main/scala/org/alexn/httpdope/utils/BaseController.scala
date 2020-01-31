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

package org.alexn.httpdope.utils

import cats.effect.Sync
import cats.implicits._
import io.circe.{Decoder, Encoder, Json, Printer}
import org.http4s
import org.http4s.circe.CirceInstances
import org.http4s.dsl.Http4sDsl
import org.http4s.twirl.TwirlInstances
import org.http4s.{EntityDecoder, EntityEncoder, InvalidMessageBodyFailure}

class BaseController[F[_] : Sync] extends BaseController.InstancesLevel0[F]
  with Http4sDsl[F]
  with LazyLogging
  with TwirlInstances {

  private[BaseController] val circeInstances =
    CirceInstances.withPrinter(Printer.spaces2).build

  protected implicit def jsonDecoder: EntityDecoder[F, Json] =
    circeInstances.jsonDecoder

  protected implicit def jsonEncoder: EntityEncoder[F, Json] =
    circeInstances.jsonEncoder

  protected def timeout[A : Encoder](key: String, value: A) =
    RequestTimeout(Json.obj(
      "status" -> Json.fromString("request-timeout"),
      key -> Encoder[A].apply(value)
    ))

  protected def notFound[A : Encoder](key: String, value: A) =
    NotFound(Json.obj(
      "status" -> Json.fromString("not-found"),
      key -> Encoder[A].apply(value)
    ))
}

object BaseController {
  /** Low-level instances. */
  private[BaseController] abstract class InstancesLevel0[F[_] : Sync] {
    self: BaseController[F] =>

    implicit def entityDecoder[A : Decoder]: EntityDecoder[F, A] =
      circeInstances.jsonDecoder[F](Sync[F]).flatMapR { json =>
        http4s.DecodeResult(Sync[F].delay {
          json.as[A].leftMap { err =>
            InvalidMessageBodyFailure(
              s"Could not decode JSON: $json",
              Some(err))
          }
        })
      }

    implicit def entityEncoder[A : Encoder]: EntityEncoder[F, A] =
      circeInstances.jsonEncoder[F](Sync[F]).contramap(Encoder[A].apply)
  }
}
