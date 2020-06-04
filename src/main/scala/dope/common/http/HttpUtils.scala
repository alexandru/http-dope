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

import io.circe.generic.JsonCodec
import io.circe.parser.decode
import org.http4s.Uri.Scheme
import org.http4s.headers.{Host, `X-Forwarded-Proto`}
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Request, Response}
import scala.concurrent.duration.Duration

object HttpUtils {
  /**
    * Fetch a header from the request.
    */
  def getHeader[F[_]](request: Request[F], name: String): Option[String] = {
    request.headers.get(CaseInsensitiveString(name)).map(_.value)
  }

  def getRootURL[F[_]](r: Request[F]): Option[String] = {
    r.headers.get(Host) match {
      case Some(host) =>
        val scheme = getForwardedProto(r).getOrElse(Scheme.http)
        Some(scheme.value + "://" + host.value)
      case _ =>
        None
    }
  }

  def getForwardedProto[F[_]](r: Request[F]): Option[Scheme] = {
    r.headers.get(CaseInsensitiveString("Cf-Visitor"))
      .flatMap(h => decode[CFVisitor](h.value).toOption.flatMap(_.scheme))
      .orElse(r.headers.get(`X-Forwarded-Proto`).map(_.value))
      .flatMap(Scheme.fromString(_).toOption)
  }

  def cached[F[_]](expiry: Duration, isPublic: Boolean = true)(r: Response[F]): Response[F] = {
    r.putHeaders(Header("Cache-Control",
      if (expiry.isFinite) {
        val cacheType = if (isPublic) "public" else "private"
        s"$cacheType, max-age=${expiry.toSeconds}, stale-while-revalidate=${expiry.toSeconds}"
      } else {
        "no-cache"
      }
    ))
  }

  @JsonCodec
  final case class CFVisitor(scheme: Option[String])
}
