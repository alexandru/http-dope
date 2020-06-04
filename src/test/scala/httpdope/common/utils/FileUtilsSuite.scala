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

package dope
package common.utils

import monix.eval.Task

class FileUtilsSuite extends AsyncBaseSuite.OfTask {
  testEffect("extract file1.txt from .tar.gz") {
    read(1)
  }

  testEffect("extract file2.txt from .tar.gz") {
    read(2)
  }

  def read(fileNr: Int) = {
    val file = for {
      service <- FileUtils.test
      res = Task(getClass.getResourceAsStream("/test.tar.gz"))
      file <- service.extractFileFromTarStream(s".*?file$fileNr\\.txt$$".r, res)
    } yield {
      (service, file)
    }

    file.use {
      case (_, None) =>
        fail("File not found")
      case (service, Some(file)) =>
        for {
          text <- service.readAllTextFromFile(file)
        } yield {
          assert(text.trim ===
            s"""
               |Text file $fileNr.
               |Line 2.
               |""".stripMargin.trim)
        }
    }
  }
}
