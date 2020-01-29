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
import cats.data.Kleisli
import cats.implicits._
import org.http4s._

object CORSMiddleware {
  /**
    * An HTTP middleware that adds CORS headers to requests.
    */
  def apply[F[_], G[_]](http: Http[F, G])(implicit F: Applicative[F]): Http[F, G] =
    Kleisli { req =>
      /*
      val allow = req.headers
        .get(CaseInsensitiveString("Origin"))
        .fold("*")(_.value)
       */
      http(req).map { resp =>
        resp.putHeaders(
          Header("Access-Control-Allow-Methods", "GET, POST, OPTIONS"),
          Header("Access-Control-Allow-Credentials", "true"),
          Header("Access-Control-Allow-Origin", "*"),
          Header("Access-Control-Allow-Headers", "Content-Type, *")
        )
      }
    }
}
