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

package engine

import javafx.stage.Window

import scala.collection.immutable.ListMap
import java.awt.Robot
import java.awt.event.KeyEvent

sealed trait MoveDirection
case object Clockwise extends MoveDirection
case object Counterclockwise extends MoveDirection

sealed trait MoveAttemptOutcome
case object BlockEntered extends MoveAttemptOutcome
case class ChoiceGroupSelected(from: Int, direction: MoveDirection) extends MoveAttemptOutcome
case object ChoiceSelected extends MoveAttemptOutcome
case object MoveAborted extends MoveAttemptOutcome


class Engine {
  import engine.MoveDirection
  import engine.MoveAttemptOutcome

  private var currentIndex = 0
  private var previousIndex = 0
  private var choiceStartIndex = 0
  private var choiceEndIndex = 0

  private var _isPicking = false
  private var moveDirection: Option[MoveDirection] = None
  private val robot = new Robot

  def isPicking(): Boolean = {
    if(_isPicking) println("Picking...")
    _isPicking
  }
  def isPicking_=(isPicking: Boolean) = {
    _isPicking = isPicking
  }

def choices: ListMap[(Int, Int, MoveDirection), String] = ListMap(
    (1, 1, Clockwise)->"a", (1, 2, Clockwise)->"b", (1, 3, Clockwise)->"c", (1, 4, Clockwise)->"d",
    (2, 1, Clockwise)->"e", (2, 2, Clockwise)->"f", (2, 3, Clockwise)->"g", (2, 4, Clockwise)->"i",
    (3, 1, Clockwise)->"j", (3, 2, Clockwise)->"k", (3, 3, Clockwise)->"l", (3, 4, Clockwise)->"m",
    (4, 1, Clockwise)->"n", (4, 2, Clockwise)->"o", (4, 3, Clockwise)->"p", (4, 4, Clockwise)->"q",
    (1, 1, Counterclockwise) -> "r", (1, 2, Counterclockwise) -> "s", (1, 3, Counterclockwise) -> "t",
    (1, 4, Counterclockwise) -> "u", (2, 1, Counterclockwise) -> "v", (2, 2, Counterclockwise) -> "w",
    (2, 3, Counterclockwise) -> "x", (2, 4, Counterclockwise) -> "y", (3, 1, Counterclockwise) -> "z",
    (3, 2, Counterclockwise) -> "space", (3, 3, Counterclockwise) -> ".", (3, 4, Counterclockwise) -> "⌫",
    (4, 1, Counterclockwise) -> "?", (4, 2, Counterclockwise) -> ",", (4, 3, Counterclockwise) -> "↵",
    (4, 4, Counterclockwise) -> "h"
  )

  def makeMove(index: Int): MoveAttemptOutcome = {
    var valid = false
    var moveAttemptOutcome: MoveAttemptOutcome = MoveAborted

    println(s"\nTry: move from $currentIndex (it's previous is $previousIndex) to $index (${moveDirection.toString})")

    if (!isPicking() || previousIndex == index && currentIndex != 0 || index == 0 && previousIndex == 0)
      resetPicking()
    else {
      valid = true
      moveAttemptOutcome = BlockEntered
      if (currentIndex == 0)
        choiceStartIndex = index
      else if (moveDirection.isEmpty) {
        moveDirection = (currentIndex, index) match {
          case (1, 2) | (2, 3) | (3, 4) | (4, 1) => Option(Clockwise)
          case _ => Option(Counterclockwise)
        }

        moveAttemptOutcome = ChoiceGroupSelected(currentIndex, moveDirection.get)
      }
      else if (index == 0) {
        choiceEndIndex = currentIndex
        println(s"$choiceStartIndex, $choiceEndIndex, ${moveDirection.get}")

        val choice: String = choices(choiceStartIndex, choiceEndIndex, moveDirection.get)

        choice match {
          case "space" => {
            robot.keyPress(KeyEvent.VK_SPACE)
            robot.keyRelease(KeyEvent.VK_SPACE)
          }
          case "⌫" => {
            robot.keyPress(KeyEvent.VK_BACK_SPACE)
            robot.keyRelease(KeyEvent.VK_BACK_SPACE)
          }
          case "↵" => {
            robot.keyPress(KeyEvent.VK_SHIFT)
            robot.keyPress(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_SHIFT)
            typeString("- Written using CycPick")
            robot.keyPress(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_ENTER)
          }
          case _ => typeString(choice)
        }

        resetChoise()
        moveAttemptOutcome = ChoiceSelected
      }
      previousIndex = currentIndex
      currentIndex = index
    }

    println(s"\tmove is%s valid".format(if valid  then "" else " not"))

    moveAttemptOutcome
  }

  def getCurrentIndex(): Int = currentIndex

  def resetChoise(): Unit = {
    moveDirection = None
    choiceStartIndex = 0
    choiceEndIndex = 0
  }

  def resetPicking(): Unit = {
    moveDirection = None
    isPicking = false
    previousIndex = 0
    currentIndex = 0
  }
  def leftProgramWindow(): Unit = {
    resetPicking()
  }

  // Return focus to previous tab
  def returnFocus(): Unit = {
    robot.keyPress(KeyEvent.VK_ALT)
    robot.keyPress(KeyEvent.VK_TAB)
    robot.delay(30) //set the delay
    robot.keyRelease(KeyEvent.VK_ALT)
    robot.keyRelease(KeyEvent.VK_TAB)
  }

  // snipped from https://alvinalexander.com/scala/scala-java-robot-class-example-boulder-colorado/
  private def typeString(s: String) = {
    val bytes = s.getBytes
    for (b <- bytes) {
      var code = b.toInt
      // keycode only handles [A-Z] (which is ASCII decimal [65-90])
      if (code > 96 && code < 123)
        code = code - 32
      robot.keyPress(code)
      robot.keyRelease(code)
    }
  }
}
