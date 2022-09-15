/*
    This file is part of CycPick.
    Copyright (C) 2022  Srđan Todorović

    CycPick is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CycPick is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.

    Contact: tsrdjan@pm.me
*/

package engine.choice

import engine.choice.ChoiceType._
import engine.choice.{ChoiceGroup, ChoiceType, ChoiceTypeSerializer}
import org.json4s.*
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, writePretty}
import java.io.{File, PrintWriter}
import scala.io.Source

implicit val formats: Formats = DefaultFormats + new ChoiceGroupSerializer + new ChoiceTypeSerializer

object ChoiceType extends Enumeration {
  type ChoiceType = Value
  val OneTimeUse, TextWriter, ChoiceWithSubchoices  = Value
}

object Choice {
  val defaultSavePath = "choices.json"
  private var count = 0
  private def increment = {
    count += 1
    count
  }

  def defaultLayout: Tuple2[String, Choice] = Tuple2("0",
    new Choice(0, "keyboard", "", ChoiceWithSubchoices, Option(List(
      new ChoiceGroup(1, List(
        new Choice(1, "x", "x"),
        new Choice(2, "m", "m"),
        new Choice(3, "r", "r"),
        new Choice(4, "o", "o")
      )),
      new ChoiceGroup(2, List(
        new Choice(1, "y", "y"),
        new Choice(2, "i", "i"),
        new Choice(3, "t", "t"),
        new Choice(4, "c", "c")
      )),
      new ChoiceGroup(3, List(
        new Choice(1, "n", "n"),
        new Choice(2, "w", "w"),
        new Choice(3, "k", "k"),
        new Choice(4, "d", "d")
      )),
      new ChoiceGroup(4, List(
        new Choice(1, "v", "v"),
        new Choice(2, "q", "q"),
        new Choice(3, "e", "e"),
        new Choice(4, "j", "j")
      )),
      new ChoiceGroup(5, List(
        new Choice(1, "l", "l"),
        new Choice(2, "s", "s"),
        new Choice(3, "⇧", "shift"),
        new Choice(4, "b", "b")
      )),
      new ChoiceGroup(6, List(
        new Choice(1, "f", "f"),
        new Choice(2, "p", "p"),
        new Choice(3, ".", ". "),
        new Choice(4, "⌫", "backspace")
      )),
      new ChoiceGroup(7, List(
        new Choice(1, "z", "z"),
        new Choice(2, "h", "h"),
        new Choice(3, "space", " "),
        new Choice(4, "↵", "enter")
      )),
      new ChoiceGroup(8, List(
        new Choice(1, "a", "a"),
        new Choice(2, "u", "u"),
        new Choice(3, "g", "g"),
        new Choice(4, ",", ", ")
      ))
    ))))

  def saveToFile(choice: Tuple2[String,Choice], path: String = defaultSavePath): Unit = {
    val file = new File(path)
    val pw = new PrintWriter(file)
    pw.write(writePretty(choice)) // TODO handle exceptions
    pw.close()
  }

  def loadFromFile(path: String = defaultSavePath): Tuple2[String,Choice] = {
    val source = Source.fromFile(path)
    val lines = try source.mkString finally source.close()
    read[Tuple2[String,Choice]](lines)
  }
}

// TODO change Option[List[Choice]] to list of indexes instead of indexes
class Choice(
  val placeInGroup: Int,
  val label: String,
  val content: String,
  val choiceType: ChoiceType = TextWriter,
  val subchoiceGroups: Option[List[ChoiceGroup]] = None // TODO Change to Map
  ) {

  private def createChoiceOptions(): List[Choice] = {
    List()
  }

  def unload(): Unit = {

  }
}
