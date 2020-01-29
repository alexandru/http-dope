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

import cats.Applicative
import org.alexn.httpdope.config.HttpServerConfig
import org.http4s.Http
import org.http4s.server.middleware.{HttpsRedirect => Implementation}

object HttpsRedirect {

  def apply[F[_], G[_]](config: HttpServerConfig)(http: Http[F, G])(implicit F: Applicative[F]): Http[F, G] =
    if (config.forceHTTPS) Implementation(http) else http
}
