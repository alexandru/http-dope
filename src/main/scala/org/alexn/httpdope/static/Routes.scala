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

package org.alexn.httpdope.static

import cats.effect.{Blocker, ContextShift, Sync}
import org.http4s.{HttpRoutes, Request, Response, StaticFile}
import org.http4s.dsl.Http4sDsl

final class Routes[F[_]](blocker: Blocker)(implicit F: Sync[F], cs: ContextShift[F]) {
  val dsl = new Http4sDsl[F] {}
  import dsl._

  def staticRoutes =
    HttpRoutes.of[F] {
      case GET -> Root =>
        Ok("Hello!")

      case GET -> Root / "hello" / name =>
        Ok(s"Hello, ${name.capitalize}!")

      case request @ GET -> Root / "favicon.ico" =>
        serveStaticFile("favicon.ico", request)

      case request @ GET -> "assets" /: ValidPath(path) =>
        serveStaticFile(path, request)
    }

  def serveStaticFile(resourcePath: String, r: Request[F]) =
    StaticFile.fromResource(s"/assets/${resourcePath}", blocker, Some(r))
      .getOrElse(Response.notFound)

  object ValidPath {
    def unapply(ref: Path): Option[String] =
      ref.toList match {
        case Nil | ("" | "/") :: Nil => None
        case other if !other.contains("..") => Some(other.mkString)
        case _ => None
      }
  }
}

object Routes {
  def apply[F[_]](blocker: Blocker)(implicit F: Sync[F], cs: ContextShift[F]) =
    new Routes[F](blocker)

}
