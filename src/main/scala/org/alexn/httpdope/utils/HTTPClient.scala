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

import java.io.File
import cats.effect.{Blocker, Concurrent, ConcurrentEffect, ContextShift, Resource}
import cats.implicits._
import org.http4s.EntityDecoder
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import monix.execution.Scheduler

final class HTTPClient[F[_]] private (client: Client[F], blocker: Blocker)
  (implicit F: Concurrent[F], cs: ContextShift[F]) {

  /**
    * Downloads a binary file from a given URL.
    */
  def downloadFileFromURL(
    url: String,
    tmpFilePrefix: String,
    tmpFileSuffix: String): Resource[F, File] = {

    Resource(F.suspend {
      val file = File.createTempFile(tmpFilePrefix, tmpFileSuffix)
      file.deleteOnExit()
      implicit val enc = EntityDecoder.binFile(file, blocker)

      for {
        handle <- client.expect[File](url)
      } yield {
        (handle, F.delay { handle.delete(); () })
      }
    })
  }
}

object HTTPClient {
  /**
    * Creates a test instance, to be used in the console.
    */
  def test[F[_]](implicit F: ConcurrentEffect[F], cs: ContextShift[F]): Resource[F, HTTPClient[F]] = {
    import Scheduler.global
    for {
      client <- BlazeClientBuilder[F](global).resource
      (_, blocker) <- Schedulers.createBlockingContext()
    } yield {
      new HTTPClient(client, blocker)
    }
  }
}
