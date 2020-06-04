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

package dope.common.utils

import cats.implicits._
import cats.effect.{Blocker, Resource, Sync}
import monix.execution.Scheduler
import monix.execution.schedulers.SchedulerService
import scala.concurrent.duration._

object Schedulers {
  /**
    * Creates a `monix.execution.Scheduler` as a `Resource` that can
    * be safely closed.
    *
    * {{{
    *   Schedulers.resource(Task(Scheduler.io()))
    * }}}
    */
  def resource[F[_]](ref: F[SchedulerService])(implicit F: Sync[F]): Resource[F, Scheduler] =
    Resource {
      for {
        sc <- ref
      } yield {
        (sc, F.delay {
          sc.shutdown()
          sc.awaitTermination(10.minutes)
          ()
        })
      }
    }

  /**
    * Creates an execution context meant for blocking threads.
    */
  def createBlockingContext[F[_]](name: String = "blocking-io")
    (implicit F: Sync[F]): Resource[F, (Scheduler, Blocker)] = {

    for {
      sc <- resource(F.delay(Scheduler.io(name)))
    } yield {
      (sc, Blocker.liftExecutionContext(sc))
    }
  }
}
