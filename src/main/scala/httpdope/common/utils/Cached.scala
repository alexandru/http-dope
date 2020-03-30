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

import cats.effect.concurrent.Deferred
import cats.effect.{Clock, Concurrent}
import cats.implicits._
import monix.execution.atomic.Atomic

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

final class Cached[F[_], A] private (implicit F: Concurrent[F], clock: Clock[F])
  extends StrictLogging {

  private type State = Map[String, (Deferred[F, A], Long)]
  private[this] val state = Atomic(Map.empty : State)

  /**
    * Fetches the current cached value, or initiates a task evaluation to
    * update the current value.
    */
  def getOrUpdate(key: String, exp: FiniteDuration, task: F[A]): F[A] = {
    def update(current: State, nowMs: Long): F[A] = {
      val promise = Deferred.unsafe[F, A]
      val expiresAt = nowMs + exp.toMillis
      val update = current.updated(key, (promise, expiresAt))
      if (state.compareAndSet(current, update)) {
        F.bracket(task)(F.pure)(promise.complete)
      } else {
        F.suspend(loop())
      }
    }

    def loop(): F[A] =
      clock.monotonic(MILLISECONDS).flatMap { nowMs =>
        val current = state.get()

        current.get(key) match {
          case None =>
            update(current, nowMs)
          case Some((p, expiresAt)) =>
            if (nowMs < expiresAt) {
              logger.info("Cache hit: " + key)
              p.get
            } else {
              update(current, nowMs)
            }
        }
      }
    loop()
  }
}

object Cached {
  /**
    * Builds a [[Cached]] instance.
    */
  def apply[F[_], A](implicit F: Concurrent[F], clock: Clock[F]): F[Cached[F, A]] =
    Concurrent[F].delay(new Cached())
}
