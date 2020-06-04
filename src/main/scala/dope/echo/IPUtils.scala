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

package dope.echo

import java.net.InetAddress

import dope.common.http.HttpUtils
import dope.common.http.HttpUtils
import dope.common.models.IP
import org.http4s.Request

import scala.util.control.NonFatal

/**
  * Some utilities for detecting IPs from the request.
  */
object IPUtils {
  /**
    * Returns `true` if the given IP is a public one.
    */
  def isPublicIP(ip: IP): Boolean = {
    try {
      val parsed = InetAddress.getByName(ip.value)
      !(parsed.isLoopbackAddress || parsed.isSiteLocalAddress)
    } catch {
      case NonFatal(_) => false
    }
  }

  /**
    * Extract IP from the request.
    */
  def extractClientIP[F[_]](request: Request[F]): Option[IP] =
    HttpUtils.getHeader(request, "X-Forwarded-For") match {
      case Some(header) =>
        header.split("\\s*,\\s*").find(ip => isPublicIP(IP(ip))) match {
          case Some(ip) =>
            Some(IP(ip))
          case None =>
            request.remoteAddr.map(IP(_))
        }
      case None =>
        request.remoteAddr.map(IP(_))
    }
}
