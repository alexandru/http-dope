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

package dope.common.http

import cats.effect.Sync
import org.http4s.HttpRoutes

/**
  * An empty controller is useful for those cases in which
  * we don't want to expose a subset of the routes, based on
  * the app's configuration.
  */
final class EmptyController[F[_]](implicit F: Sync[F]) extends ControllerLike[F] {
  def routes: HttpRoutes[F] =
    HttpRoutes.of[F](PartialFunction.empty)
}
