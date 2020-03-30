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

package httpdope.common.http

import cats.implicits._
import cats.effect.Sync
import io.circe.{Encoder, Json}
import org.http4s.{HttpRoutes, Response, Status}
import io.circe.syntax._

/**
  * Message.
  */
final class StaticController[F[_], A](
  status: Status,
  message: A)
  (implicit F: Sync[F], A: Encoder[A]) extends BaseController[F] {

  def routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case _ =>
        Response[F](status)
          .withEntity(message.asJson)
          .pure[F]
    }
}

object StaticController {
  def apply[F[_]] = new PartialApply[F]

  final class PartialApply[F[_]](val dummy: Boolean = false) extends AnyVal {
    def apply[A](status: Status, message: A)
      (implicit F: Sync[F], A: Encoder[A]): ControllerLike[F] = {

      new StaticController(status, message)
    }

    def serviceUnavailable[A](reason: A)
      (implicit F: Sync[F], A: Encoder[A]): ControllerLike[F] = {

      new StaticController(
        Status.ServiceUnavailable,
        Json.obj(
          "status" -> Json.fromString("service-unavailable"),
          "reason" -> A.apply(reason)
        )
      )
    }
  }
}
