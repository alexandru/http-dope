package org.alexn.httpdope.utils

import cats.implicits._
import cats.effect._
import java.io._
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import scala.util.matching.Regex

object FileUtils {
  /**
    * Extracts a file from a `.tar.gz` archive whose name matches a given pattern.
    */
  def extractFileFromTarFile[F[_]](pattern: Regex, blocker: Blocker, file: File)
    (implicit F: Sync[F], cs: ContextShift[F]): Resource[F, Option[File]] = {

    for {
      in <- Resource.fromAutoCloseable(blocker.blockOn(F.delay(new FileInputStream(file))))
      rf <- FileUtils.extractFileFromTarStream(pattern, blocker, F.pure(in))
    } yield {
      rf
    }
  }

  /**
    * Extracts a file from a `.tar.gz` archive whose name matches a given pattern.
    */
  def extractFileFromTarStream[F[_]](pattern: Regex, blocker: Blocker, in: F[InputStream])
    (implicit F: Sync[F], cs: ContextShift[F]): Resource[F, Option[File]] = {

    val archive = for {
      in     <- Resource.fromAutoCloseable(blocker.blockOn(in))
      gzipIn <- Resource.fromAutoCloseable(blocker.blockOn(F.delay(new GzipCompressorInputStream(in))))
      tarIn  <- Resource.fromAutoCloseable(blocker.blockOn(F.delay(new TarArchiveInputStream(gzipIn))))
      entry  <- Resource.liftF(findTarEntry(pattern, tarIn, blocker))
    } yield {
      (entry, tarIn)
    }

    archive.flatMap {
      case (Some(entry), tarIn) =>
        val out = for {
          file <- createTempFile("extracted", entry.getName, blocker)
          out <- Resource.fromAutoCloseable(blocker.blockOn(F.delay(new FileOutputStream(file))))
        } yield {
          (file, out)
        }

        out.evalMap { case (outFile, out) =>
          F.delay {
            val buffer = new Array[Byte](4096)
            var readCount = 0
            do {
              readCount = tarIn.read(buffer)
              if (readCount >= 0) {
                out.write(buffer, 0, readCount)
              }
            } while (readCount > 0)

            out.close()
            Some(outFile)
          }
        }

      case (None, _) =>
        Resource.pure(None)
    }
  }

  private def findTarEntry[F[_]](pattern: Regex, tarIn: TarArchiveInputStream, blocker: Blocker)
    (implicit F: Sync[F], cs: ContextShift[F]) = {

    F.delay {
      var continue = true
      var found = Option.empty[ArchiveEntry]
      while (continue) {
        val entry = tarIn.getNextEntry
        if (entry == null) {
          continue = false
        } else if (!entry.isDirectory && pattern.matches(entry.getName)) {
          continue = false
          found = Some(entry)
        }
      }
      found
    }
  }

  /**
    * Creates a temporary file.
    */
  def createTempFile[F[_]](prefix: String, suffix: String, blocker: Blocker)
    (implicit F: Sync[F], cs: ContextShift[F]): Resource[F, File] = {

    Resource(blocker.blockOn(
      F.delay {
        val file = File.createTempFile(prefix, suffix)
        file.deleteOnExit()
        (file, blocker.blockOn(F.delay(file.delete())))
      }
    ))
  }
}
