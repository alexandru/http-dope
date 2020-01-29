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

package org.alexn.httpdope.config

import cats.effect.{Effect, IO, Resource}
import monix.eval.Task
import monix.execution.Scheduler
import org.scalactic.source
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.{Tag, compatible}
import scala.concurrent.{Future, Promise}

trait AsyncBaseSuite[F[_]] extends AsyncFunSuite {
  implicit def F: Effect[F]

  protected def testEffect(testName: String, testTags: Tag*)(testFun: => F[compatible.Assertion])(implicit pos: source.Position): Unit =
    test(testName, testTags:_*)(unsafeToFuture(testFun))

  protected def withSystemProperty(key: String, value: Option[String]): Resource[F, Unit] =
    Resource(F.delay {
      val oldValue = Option(System.getProperty(key)).filter(_.nonEmpty)
      value.fold(System.clearProperty(key))(System.setProperty(key, _))
      val reset = F.delay { oldValue.fold(System.clearProperty(key))(System.setProperty(key, _)); () }
      ((), reset)
    })

  private[this] def unsafeToFuture[A](fun: F[A]): Future[A] = {
    val p = Promise[A]()
    F.runAsync(fun)(result => IO(p.complete(result.toTry))).unsafeRunSync()
    p.future
  }
}

object AsyncBaseSuite {
  trait OfTask extends AsyncBaseSuite[Task] {
    import Scheduler.Implicits.global
    implicit val F: Effect[Task] = Task.catsEffect
  }
}
