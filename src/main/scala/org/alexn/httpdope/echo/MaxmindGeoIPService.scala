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

import cats.implicits._
import java.io.FileNotFoundException
import java.net.InetAddress
import java.util.zip.GZIPInputStream

import com.maxmind.geoip2.record._
import cats.effect.{Async, Blocker, ConcurrentEffect, ContextShift, Resource}
import com.maxmind.geoip2.DatabaseReader
import org.alexn.httpdope.config.AppConfig
import org.alexn.httpdope.utils.{FileUtils, HTTPClient, LazyLogging, Schedulers}
import org.http4s.client.blaze.BlazeClientBuilder

import scala.util.Try

final class MaxmindGeoIPService[F[_]] private (db: DatabaseReader, blocker: Blocker)
  (implicit F: Async[F], cs: ContextShift[F]) {

  def findIP(ip: String): F[Option[GeoIPInfo]] =
    F.delay(InetAddress.getByName(ip)).flatMap(findByAddress)

  def findByAddress(address: InetAddress): F[Option[GeoIPInfo]] = {
    def country(ref: Country): Option[GeoIPCountry] = {
      if (ref != null && ref.getIsoCode != null)
        Some(
          GeoIPCountry(
            isoCode = ref.getIsoCode,
            name = Option(ref.getName).filter(_.nonEmpty),
            isInEuropeanUnion = ref.isInEuropeanUnion
          ))
      else
        None
    }

    def city(ref: City) =
      if (ref != null && ref.getName != null)
        Some(GeoIPCity(name = ref.getName))
      else
        None

    def location(ref: Location) =
      if (ref != null && ref.getLatitude != null && ref.getLongitude != null)
        Some(
          GeoIPLocation(
            latitude = ref.getLatitude.doubleValue(),
            longitude = ref.getLongitude.doubleValue(),
            timezone = Option(ref.getTimeZone),
            accuracyRadius = Option(ref.getAccuracyRadius).map(_.intValue())
          ))
      else
        None

    def postal(ref: Postal) =
      if (ref != null && ref.getCode != null)
        Some(GeoIPPostalCode(code = ref.getCode))
      else
        None

    def continent(ref: Continent) =
      if (ref != null && ref.getCode != null)
        Some(GeoIPContinent(code = ref.getCode, name = Option(ref.getName)))
      else
        None

    if (address.isSiteLocalAddress || address.isLoopbackAddress) {
      F.pure(None)
    } else {
      blocker.blockOn(F.delay {
        val cityInfo = db.city(address)
        Some(
          GeoIPInfo(
            ip = IP(address.getHostAddress),
            country = country(cityInfo.getCountry),
            registeredCountry = country(cityInfo.getRegisteredCountry),
            representedCountry = country(cityInfo.getRepresentedCountry),
            continent = continent(cityInfo.getContinent),
            city = city(cityInfo.getCity),
            location = location(cityInfo.getLocation),
            postal = postal(cityInfo.getPostal),
          ))
      })
    }
  }
}

object MaxmindGeoIPService extends LazyLogging {
  /**
    * Builder that can build the service with a fresh download of the Maxmind database.
    *
    * (if configured as such in [[MaxmindGeoIPConfig]])
    */
  def apply[F[_]](config: MaxmindGeoIPConfig, client: HTTPClient[F], blocker: Blocker)
    (implicit F: Async[F], cs: ContextShift[F]): Resource[F, MaxmindGeoIPService[F]] = {

    if (!config.refreshDBOnRun) {
      if (getClass.getResource(localResource) != null) {
        fromLocalResource(blocker)
      } else {
        Resource.liftF(F.delay {
          logger.warn(s"Local Maxmind DB doesn't exist ($localResource), retrying live")
        }).flatMap { _ =>
          // Retry
          apply(config.copy(refreshDBOnRun = true), client, blocker)
        }
      }
    } else {
      val url = downloadURL(config)
      val files = FileUtils(blocker)

      val resource = for {
        _ <- Resource.liftF(F.delay { logger.info(s"Downloading database: ${config.edition}")})
        tarFile <- client.downloadFileFromURL(url, config.edition.id, ".tar.gz")
        _ <- Resource.liftF(F.delay { logger.info(s"Extracting database file ${tarFile.getName}")})
        dbFile <- files.extractFileFromTarFile("^.*\\.mmdb$".r, tarFile)
      } yield {
        logger.info("Database available, initiating MaxmindGeoIPService")
        val file = dbFile.getOrElse(throw new FileNotFoundException(".mmdb database"))
        val db = new DatabaseReader.Builder(file).build()
        new MaxmindGeoIPService[F](db, blocker)
      }

      resource.handleErrorWith { error =>
        logger.error(error)("Could not initialize Maxmind DB from URL")
        fromLocalResource(blocker)
      }
    }
  }

  def fromLocalResource[F[_]](blocker: Blocker)
    (implicit F: Async[F], cs: ContextShift[F]): Resource[F, MaxmindGeoIPService[F]] = {

    val res = F.delay {
      logger.info(s"Initializing Maxmind DB from local resource $localResource")
      new GZIPInputStream(getClass.getResourceAsStream(localResource))
    }
    for {
      in <- Resource.fromAutoCloseable(res)
    } yield {
      val db = new DatabaseReader.Builder(in).build()
      new MaxmindGeoIPService[F](db, blocker)
    }
  }

  /**
    * Creates a test instance, for playing around in the console.
    */
  def test[F[_]](config: Option[AppConfig])
    (implicit F: ConcurrentEffect[F], cs: ContextShift[F]): Resource[F, MaxmindGeoIPService[F]] = {

    import monix.execution.Scheduler.global
    for {
      (_, blocker) <- Schedulers.createBlockingContext[F]()
      client <- BlazeClientBuilder[F](global).resource
      config <- Resource.liftF(config.fold(AppConfig.loadFromEnv)(F.pure))
      httpClient = HTTPClient(client, blocker)
      ref <- apply(config.maxmindGeoIP, httpClient, blocker)
    } yield {
      ref
    }
  }

  private def localResource = "/maxmind.mmdb.gz"
  private def downloadURL(cfg: MaxmindGeoIPConfig): String =
    s"https://download.maxmind.com/app/geoip_download?edition_id=${cfg.edition.id}&license_key=${cfg.apiKey.value}&suffix=tar.gz"
}
