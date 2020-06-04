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

package dope.common.utils

import java.io._
import java.nio.charset.Charset

import cats.effect._
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

import scala.util.matching.Regex

/**
  * Blocking I/O operations.
  */
final class FileUtils[F[_]] private (blocker: Blocker)
  (implicit F: Sync[F], cs: ContextShift[F]) {

  /**
    * Extracts a file from a `.tar.gz` archive whose name matches a given pattern.
    */
  def extractFileFromTarFile(pattern: Regex, file: File): Resource[F, Option[File]] =
    for {
      in <- Resource.fromAutoCloseable(blocker.blockOn(F.delay(new FileInputStream(file))))
      rf <- extractFileFromTarStream(pattern, F.pure(in))
    } yield {
      rf
    }

  /**
    * Extracts a file from a `.tar.gz` archive whose name matches a given pattern.
    */
  def extractFileFromTarStream(pattern: Regex, in: F[InputStream]): Resource[F, Option[File]] = {
    import FileUtils.ExtractFileName

    val archive = for {
      in     <- Resource.fromAutoCloseable(blocker.blockOn(in))
      gzipIn <- Resource.fromAutoCloseable(blocker.blockOn(F.delay(new GzipCompressorInputStream(in))))
      tarIn  <- Resource.fromAutoCloseable(blocker.blockOn(F.delay(new TarArchiveInputStream(gzipIn))))
      entry  <- Resource.liftF(findTarEntry(pattern, tarIn))
    } yield {
      (entry.map(_.getName), tarIn)
    }

    archive.flatMap {
      case (Some(ExtractFileName(entryName)), tarIn) =>
        val out = for {
          file <- createTempFile("extracted", entryName)
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

      case _ =>
        Resource.pure(None)
    }
  }

  private def findTarEntry(pattern: Regex, tarIn: TarArchiveInputStream) =
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

  /**
    * Creates a temporary file.
    */
  def createTempFile(prefix: String, suffix: String): Resource[F, File] =
    Resource(blocker.blockOn(
      F.delay {
        val file = File.createTempFile(prefix, suffix)
        file.deleteOnExit()
        (file, blocker.blockOn(F.delay { file.delete(); () }))
      }
    ))

  /**
    * Reads all the text contained by a given file.
    */
  def readAllTextFromFile(file: File, charset: Charset = Charset.forName("UTF-8")): F[String] = {
    val openFile = F.delay(
      new BufferedReader(
        new InputStreamReader(
          new FileInputStream(file),
          charset
        )))

    Resource.fromAutoCloseable(blocker.blockOn(openFile)).use { in =>
      blocker.blockOn(F.delay {
        val buf = new StringBuilder()
        val array = new Array[Char](1024)
        var continue = true

        while (continue) {
          val charsRead = in.read(array)
          if (charsRead >= 0) {
            buf.append(new String(array, 0, charsRead))
          } else {
            continue = false
          }
        }
        buf.toString()
      })
    }
  }
}

object FileUtils {
  /** Builder. */
  def apply[F[_]](blocker: Blocker)(implicit F: Sync[F], cs: ContextShift[F]): FileUtils[F] =
    new FileUtils(blocker)(F, cs)

  /**
    * Creates a test instance to use in the console.
    */
  def test[F[_]](implicit F: Sync[F], cs: ContextShift[F]): Resource[F, FileUtils[F]] =
    for {
      (_, blocker) <- Schedulers.createBlockingContext()
    } yield {
      new FileUtils[F](blocker)
    }

  private val ExtractFileName = """.*(?:^|[/\\])([^/\\]+)$""".r
}
