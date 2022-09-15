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

import engine.choice.Choice

import javafx.stage.Window

import scala.collection.immutable.ListMap
import java.awt.Robot
import java.awt.event.KeyEvent
import java.nio.file.{Paths, Files}

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
  private var shiftPressed = false

  private var _isPicking = false
  private var moveDirection: Option[MoveDirection] = None
  private val robot = new Robot
  private var currentChoice = loadMainChoice()

  def isPicking(): Boolean = {
    if(_isPicking) println("Picking...")
    _isPicking
  }
  def isPicking_=(isPicking: Boolean) = {
    _isPicking = isPicking
  }

  def choiceLabels(from: Int, direction: MoveDirection): List[String] = {
    val groupNumber = findGroupNumber(from, direction)
    val choices = currentChoice.subchoiceGroups.get
      .find(_.groupNumber == groupNumber)
      .map(_.choices)

    val labels: IndexedSeq[String] = (1 to numOfChoicesPerGroup).flatMap(i =>
      choices.flatMap(_.find(_.placeInGroup == i)
        .map(_.label)
        .orElse(Option(missingChoiceStringRepr))
    ))

    if (labels.nonEmpty)
      labels.toList
    else
      List.fill(numOfChoicesPerGroup)(missingChoiceStringRepr)

    
  }

  private def convertMoveToChoice(from: Int, to: Int, direction: MoveDirection): Option[Choice] = {
    val groupNumber = findGroupNumber(from, direction)
    val choices = currentChoice.subchoiceGroups.get
      .find(_.groupNumber == groupNumber)
      .map(_.choices)

    choices.flatMap(_.find(_.placeInGroup == to))
  }

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

        val choice = convertMoveToChoice(choiceStartIndex, choiceEndIndex, moveDirection.get).get

        processChoice(choice)
        resetChoise()
        moveAttemptOutcome = ChoiceSelected
      }
      previousIndex = currentIndex
      currentIndex = index
    }

    println(s"\tmove is%s valid".format(if valid  then "" else " not"))

    moveAttemptOutcome
  }
  
  private def processChoice(choice: Choice): Unit = {
        choice.content match {
          case "space" => {
            robot.keyPress(KeyEvent.VK_SPACE)
            robot.keyRelease(KeyEvent.VK_SPACE)
          }
          case "backspace" => {
            robot.keyPress(KeyEvent.VK_BACK_SPACE)
            robot.keyRelease(KeyEvent.VK_BACK_SPACE)
          }
          case "enter" => {
            robot.keyPress(KeyEvent.VK_ENTER)
            robot.keyRelease(KeyEvent.VK_ENTER)
          }
          case "shift" => shiftPressed = !shiftPressed
          case _ => {
            if (!shiftPressed)
              typeString(choice.content)
            else {
              robot.keyPress(KeyEvent.VK_SHIFT)
              typeString(choice.content)
              robot.keyRelease(KeyEvent.VK_SHIFT)
              shiftPressed = !shiftPressed
            }
          }
        }
  }

  private def loadMainChoice(): Choice = {
    if (Files.exists(Paths.get(Choice.defaultSavePath)))
      Choice.loadFromFile()._2
    else {
      Choice.saveToFile(Choice.defaultLayout)
      Choice.defaultLayout._2
    }
  }

  def resetChoise(): Unit = {
    moveDirection = None
    choiceStartIndex = 0
    choiceEndIndex = 0
  }

  def resetPicking(): Unit = {
    shiftPressed = false
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

  private def findGroupNumber(blockNumber: Int, direction: MoveDirection): Int = {
    (blockNumber, direction) match {
      case (1, Counterclockwise) => 1
      case (1, Clockwise) => 2
      case (2, Counterclockwise) => 3
      case (2, Clockwise) => 4
      case (3, Counterclockwise) => 5
      case (3, Clockwise) => 6
      case (4, Counterclockwise) => 7
      case (4, Clockwise) => 8
      case _ =>
        throw new IllegalStateException(s"Unknown combination (blockNumber, direction): ($direction, $direction)")
    }
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
