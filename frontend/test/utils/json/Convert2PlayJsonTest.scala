/*
 * Copyright (C) 2017  Jens Grassel & André Schütz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package utils.json

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ MustMatchers, WordSpec }
import utils.json.Convert2PlayJson.Circe2PlayJson
import utils.json.Convert2PlayJsonTest.TestClass

class Convert2PlayJsonTest extends WordSpec with MustMatchers with PropertyChecks {
  private val testClass: Gen[TestClass] = for {
    a <- Gen.alphaNumStr
    b <- Gen.chooseNum(Int.MinValue, Int.MaxValue)
    c <- Gen.option(Gen.alphaNumStr)
  } yield TestClass(a, b, c)
  implicit private val arbitraryTestClass: Arbitrary[TestClass] = Arbitrary(testClass)

  "Circe2PlayJson" when {
    "converting to a play JsValue" must {
      "produce correct results" in {
        forAll("TestClass") { t: TestClass =>
          val circeJson      = t.asJson
          val playJson       = Circe2PlayJson.toJsValue(circeJson)
          val playJsonString = playJson.toString()
          playJsonString must include(s""""a":${t.a.asJson}""")
          playJsonString must include(s""""b":${t.b}""")
          val optC = t.c.fold("null")(c => s"""${c.asJson}""")
          playJsonString must include(s""""c":$optC""")
        }
      }
    }

    "using the syntax wrapper" must {
      "produce correct results" in {
        import Convert2PlayJson.syntax._

        forAll("TestClass") { t: TestClass =>
          val circeJson      = t.asJson
          val playJson       = circeJson.toJsValue
          val playJsonString = playJson.toString()
          playJsonString must include(s""""a":${t.a.asJson}""")
          playJsonString must include(s""""b":${t.b}""")
          val optC = t.c.fold("null")(c => s"""${c.asJson}""")
          playJsonString must include(s""""c":$optC""")
        }
      }
    }
  }

}

object Convert2PlayJsonTest {
  final case class TestClass(a: String, b: Int, c: Option[String])

  object TestClass {
    implicit val dec: Decoder[TestClass] = deriveDecoder[TestClass]
    implicit val enc: Encoder[TestClass] = deriveEncoder[TestClass]
  }
}
