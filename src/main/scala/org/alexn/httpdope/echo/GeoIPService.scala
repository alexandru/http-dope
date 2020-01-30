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

package org.alexn.httpdope.echo

import org.alexn.httpdope.utils.LazyLogging
//
//final class GeoIPService[F[_]] private (geoIP: DatabaseReader)(implicit F: Sync[F])
//  extends LazyLogging {
//
//}

object GeoIPService extends LazyLogging {

//  def downloadFile[F[_]](key: MaxmindLicenceKey, client: Client[F], blocker: Blocker)
//    (implicit F: Concurrent[F], cs: ContextShift[F]): Resource[F, File] = {
//
//    Resource(F.suspend {
//      val edition = "GeoLite2-City"
//      val file = File.createTempFile(edition, ".tar.gz")
//      file.deleteOnExit()
//
//      implicit val enc = EntityDecoder.binFile(file, blocker)
//
//      val url = s"https://download.maxmind.com/app/geoip_download?edition_id=$edition&license_key=BUthoADD8NGkAduu&suffix=tar.gz"
//      for {
//        handle <- client.expect[File](url)
//      } yield {
//        (handle, F.delay(handle.delete()))
//      }
//    })
//  }

//  def apply[F[_]](implicit F: Sync[F]): Resource[F, GeoIPService] =
//    Resource(F.delay {
//
//    })

//  lazy val geoIP: DatabaseReader = new DatabaseReader
//  .Builder(getClass.getResourceAsStream("/GeoLite2/GeoLite2-City.mmdb"))
//    .build()
//
//  def publicIP(ip: String): Boolean = {
//    try {
//      val parsed = InetAddress.getByName(ip)
//      !(parsed.isLoopbackAddress || parsed.isSiteLocalAddress)
//    } catch {
//      case NonFatal(_) => false
//    }
//  }
//
//  def getHeader[F[_]](request: Request[F], name: String): Option[String] = {
//    request.headers.get(CaseInsensitiveString(name)).map(_.value)
//  }
//
//  def getRealIP[F[_]](request: Request[F]): Option[String] =
//    request.headers.get(CaseInsensitiveString("X-Forwarded-For")) match {
//      case Some(header) =>
//        header.value.split("\\s*,\\s*").find(publicIP) match {
//          case ip @ Some(_) => ip
//          case None =>
//            request.remoteAddr
//        }
//      case None =>
//        request.remoteAddr
//    }
//
//  def getGeoIPInfo(ip: String): Json = {
//    def country(ref: Country) = {
//      if (ref != null && ref.getIsoCode != null)
//        Json.obj(
//          "isoCode" -> Option(ref.getIsoCode).fold(Json.Null)(Json.fromString),
//          "name" -> Option(ref.getName).fold(Json.Null)(Json.fromString),
//          "isInEuropeanUnion" -> Json.fromBoolean(ref.isInEuropeanUnion)
//        )
//      else
//        Json.Null
//    }
//
//    def city(ref: City) =
//      if (ref != null && ref.getName != null)
//        Json.obj(
//          "name" -> Option(ref.getName).fold(Json.Null)(Json.fromString)
//        )
//      else
//        Json.Null
//
//    def location(ref: Location) =
//      if (ref != null)
//        Json.obj(
//          "latitude" -> Json.fromDoubleOrNull(ref.getLatitude),
//          "longitude" -> Json.fromDoubleOrNull(ref.getLongitude),
//          "timezone" -> Option(ref.getTimeZone).fold(Json.Null)(Json.fromString),
//          "accuracyRadius" -> Option(ref.getAccuracyRadius).fold(Json.Null)(i => Json.fromInt(i.intValue()))
//        )
//      else
//        Json.Null
//
//    def postal(ref: Postal) =
//      if (ref != null && ref.getCode != null)
//        Json.obj(
//          "code" -> Json.fromString(ref.getCode)
//        )
//      else
//        Json.Null
//
//    def continent(ref: Continent) =
//      if (ref != null && ref.getCode != null)
//        Json.obj(
//          "code" -> Json.fromString(ref.getCode),
//          "name" -> Option(ref.getName).fold(Json.Null)(Json.fromString)
//        )
//      else
//        Json.Null
//
//    try {
//      val address = InetAddress.getByName(ip)
//      if (address.isSiteLocalAddress || address.isLoopbackAddress)
//        Json.Null
//      else {
//        val info = geoIP.city(address)
//        Json.obj(
//          "ip" -> Json.fromString(ip),
//          "country" -> country(info.getCountry),
//          "registeredCountry" -> country(info.getRegisteredCountry),
//          "representedCountry" -> country(info.getRepresentedCountry),
//          "continent" -> continent(info.getContinent),
//          "city" -> city(info.getCity),
//          "location" -> location(info.getLocation),
//          "postal" -> postal(info.getPostal)
//        )
//      }
//    } catch {
//      case NonFatal(e) =>
//        logger.error("Unexpected error", e)
//        Json.Null
//    }
//  }
}
