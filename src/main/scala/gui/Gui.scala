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

package gui

import engine.{ChoiceSelected, Clockwise, Counterclockwise, Engine, MoveAborted, MoveDirection, ChoiceGroupSelected}

import scalafx.stage.Screen
import scalafx.scene.transform.{Rotate, Translate}
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.effect.DropShadow
import scalafx.scene.text.Font
import scalafx.event.ActionEvent
import scalafx.application.JFXApp3
import scalafx.scene.*
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.scene.paint.*
import scalafx.scene.shape.*
import scalafx.scene.input.{MouseEvent, MouseButton}
import scalafx.Includes.*
import scalafx.geometry.{Insets, Pos, Rectangle2D}

import scala.util.*
import scala.collection.mutable.ListBuffer

import javafx.scene.control.Label as JLabel
import java.io.File

sealed trait GroupOrientation
case object Horizontal extends GroupOrientation
case object Vertical extends GroupOrientation

class Gui(app: JFXApp3, engine: Engine, appName: String, numOfBlocks: Int = 4) {
  private val ActiveCircleColor: Color = Color.rgb(255, 102, 0)
  private val InactiveCircleColor: Color = Color.rgb(0,82,70)
  private val BlockColor: Color = Color.rgb(76,128,118)

  private val BlockSize = 200
  private val CircleRadius = 50
  private val LabelSpacing = 15
  private val LabelMargin = 15
  private val LabelGridGap = 25
  private val LabelMaxChars = 6
  private val ChoiceGroupsOpacity = 0.35
  private val ArrowsImagePath = "resources/images/arrows.png"
  private val LogoImagePath = "resources/images/logo.png"

  private var BlockList: IndexedSeq[Node] = null
  private var MainButton: Circle = null
  private var ChoiceGroups: Group = null
  private var ChoicesOfSelectedGroup: List[Label] = null
  private var ListVerBoxes: ListBuffer[VBox] = null
  private var ListHorBoxes: ListBuffer[HBox] = null
  private var ArrowsImage: Option[ImageView] = None

  private var CurrentDirection: MoveDirection = Clockwise

  def startGui(): Unit = {
    app.stage = new JFXApp3.PrimaryStage {
      title = appName
      scene = createScene()
      resizable = false
      alwaysOnTop = true
    }
    app.stage.centerOnScreen()
    app.stage.icons += new Image(new File(LogoImagePath).toURI.toString)
  }

  private def createScene(): Scene = {
    new Scene {
      root = createStackPaneWithComponents()
      onMouseExited = () => engine.leftProgramWindow()
    }
  }

  private def createStackPaneWithComponents(): StackPane = {
    val stackPane = new StackPane

    // Create starting circle
    MainButton = new Circle {
      radius = CircleRadius
      fill = InactiveCircleColor
      effect = new DropShadow(2, 3, 3, Color.Black)
      onMouseEntered = () => if engine.isPicking() then makeMove(0)
      onMouseReleased = (e: MouseEvent) => {
        if(e.button == MouseButton.Primary) {
           engine.isPicking = !engine.isPicking() // TODO move to engine
        fill = if (engine.isPicking()) ActiveCircleColor else InactiveCircleColor
      }
      }
    }

    // Create blocks
    BlockList = for (i <- 0 until numOfBlocks) yield
      new Rectangle() {
        height = BlockSize
        width = BlockSize
        fill = BlockColor
        strokeType = StrokeType.Inside
        strokeWidth = 0.5
        stroke = Color.rgb(0, 71, 51)
        onMouseEntered = () => makeMove(i+1)
      }.asInstanceOf[Node]

    // Create block grid
    val blocks = new Group(new GridPane {
      add(BlockList(0), 0, 0)
      add(BlockList(1), 1, 0)
      add(BlockList(2), 1, 1)
      add(BlockList(3), 0, 1)
    })

    // Create layer for choice groups
    ChoiceGroups = new Group(createChoiceGroupsGrid())
    ChoiceGroups.setMouseTransparent(true)
    updateChoiceLabels()

    // Create layer for available choices in selected group
    val selectedChoicesGrid = new Group(createGridForSelectedChoiceGroup())
    selectedChoicesGrid.setMouseTransparent(true)

    // Create layer for direction indicators
    ArrowsImage = Option(new ImageView(new Image(new File(ArrowsImagePath).toURI.toString)))
    ArrowsImage.get.setMouseTransparent(true)
    ArrowsImage.get.visible = false

    // Add layers to stack pane
    stackPane.children += blocks
    stackPane.children += ChoiceGroups
    stackPane.children += selectedChoicesGrid
    stackPane.children += new Group(ArrowsImage.get)
    stackPane.children += MainButton

    // Set alignment of layers inside stack pane
    StackPane.setAlignment(ChoiceGroups, Pos.Center)
    StackPane.setAlignment(selectedChoicesGrid, Pos.Center)

    stackPane
  }

  private def createChoiceGroupsGrid(): GridPane = {
    ListVerBoxes = ListBuffer[VBox]()
    ListHorBoxes = ListBuffer[HBox]()

    // Add labels
    for (from <- 1 to numOfBlocks) {
      val v = new VBox
      val h = new HBox

      v.spacing = LabelSpacing
      h.spacing = LabelSpacing

      // Restrict width to avoid overlapping with the circle
      h.maxWidth = BlockSize - CircleRadius - LabelMargin

      ListVerBoxes += v
      ListHorBoxes += h

      for (direction <- List(Clockwise, Counterclockwise)) {
        val orient = getGroupOrientation(from, direction)
        val box = if orient == Horizontal
        then ListHorBoxes(from - 1)
        else ListVerBoxes(from - 1)

        for (to <- 1 to numOfBlocks) {
          val lbl = new Label()

          lbl.setWrapText(true)
          lbl.setMaxWidth(Double.MaxValue)

          lbl.style = s"-fx-text-fill: #c4fcf0; -fx-padding: 1"

          // Align choice text closer to the edge of the box
          lbl.alignment = Pos.Center /*(from, orient) match {
            case (i, Vertical) if i == 1 || i == 4 => Pos.CenterRight
            case (i, Vertical) if i == 2 || i == 3 => Pos.CenterLeft
            case _ => Pos.Center
          }*/

          box.children += lbl
        }
      }
    }

    // Top left block
    val borderTopLeft = new AnchorPane
    val topLeftHorGroup = new Group(ListHorBoxes(0))
    val topLeftVerGroup = new Group(ListVerBoxes(0))

    borderTopLeft.children += topLeftHorGroup
    borderTopLeft.children += topLeftVerGroup

    AnchorPane.setBottomAnchor(topLeftHorGroup, LabelMargin)
    AnchorPane.setRightAnchor(topLeftVerGroup, LabelMargin)

    // Top Right block
    val borderTopRight = new AnchorPane
    val topRightHorGroup = new Group(ListHorBoxes(1))
    val topRightVerGroup = new Group(ListVerBoxes(1))

    borderTopRight.children += topRightHorGroup
    borderTopRight.children += topRightVerGroup

    AnchorPane.setBottomAnchor(topRightHorGroup, LabelMargin)
    AnchorPane.setLeftAnchor(topRightVerGroup, LabelMargin)
    AnchorPane.setRightAnchor(topRightHorGroup, 0)

    // Bottom left block
    val borderBottomLeft = new AnchorPane
    val bottomLeftHorGroup = new Group(ListHorBoxes(3))
    val bottomLeftVerGroup = new Group(ListVerBoxes(3))

    borderBottomLeft.children += bottomLeftHorGroup
    borderBottomLeft.children += bottomLeftVerGroup

    AnchorPane.setTopAnchor(bottomLeftHorGroup, LabelMargin)
    AnchorPane.setRightAnchor(bottomLeftVerGroup, LabelMargin)
    AnchorPane.setBottomAnchor(bottomLeftVerGroup, 0)

    // Bottom Right block
    val borderBottomRight = new AnchorPane
    val bottomRightHorGroup = new Group(ListHorBoxes(2))
    val bottomRightVerGroup = new Group(ListVerBoxes(2))

    borderBottomRight.children += bottomRightHorGroup
    borderBottomRight.children += bottomRightVerGroup

    AnchorPane.setTopAnchor(bottomRightHorGroup, LabelMargin)
    AnchorPane.setLeftAnchor(bottomRightVerGroup, LabelMargin)
    AnchorPane.setRightAnchor(bottomRightHorGroup, 0)
    AnchorPane.setBottomAnchor(bottomRightVerGroup, 0)

    // Create label grid
    val grid = new GridPane {
      add(borderTopLeft, 0, 0)
      add(borderTopRight, 1, 0)
      add(borderBottomRight, 1, 1)
      add(borderBottomLeft, 0, 1)
    }

    grid.setVgap(LabelGridGap)
    grid.setHgap(LabelGridGap)
    grid.alignment = Pos.Center
    makeGridCellsEqual(grid)

    grid.alignmentInParent = Pos.Center // TODO check if this is necessary

    grid
  }

  private def createGridForSelectedChoiceGroup(): GridPane = {
    ChoicesOfSelectedGroup = List(new Label, new Label, new Label, new Label)
    ChoicesOfSelectedGroup.foreach(o => {
      o.setFont(new Font(28))
      o.effect = new DropShadow(2, 3, 3, Color.Black)
      o.alignmentInParent = Pos.Center
      o.setWrapText(true)
    })

    val grid = new GridPane {
      add(ChoicesOfSelectedGroup(0), 0, 0)
      add(ChoicesOfSelectedGroup(1), 1, 0)
      add(ChoicesOfSelectedGroup(2), 1, 1)
      add(ChoicesOfSelectedGroup(3), 0, 1)

      /// Add offset to avoid overlapping with the circle
      hgap = 2.5 * CircleRadius
      vgap = 2.5 * CircleRadius
    }

    // Add gaps to make space between arrows and the grid
    grid.hgap = LabelGridGap
    grid.vgap = LabelGridGap

    grid.alignment = Pos.Center
    makeGridCellsEqual(grid)

    grid
  }

  private def returnFocus(): Unit = {
    engine.returnFocus()
    app.stage.show()
  }

  private def makeMove(boxIndex: Int): Boolean = {
    if (engine.isPicking() && app.stage.isFocused)
      returnFocus()

    var valid = true
    val outcome = engine.makeMove(boxIndex)

    println(outcome)

    outcome match {
      case MoveAborted => valid = false
      case ChoiceGroupSelected(from, direction) => {
        ChoiceGroups.opacity = ChoiceGroupsOpacity
        ArrowsImage.get.visible = true

        // Rotate img if move is in oposite direction
        if (CurrentDirection != direction) {
          val flipTranslation = new Translate(0, ArrowsImage.get.getImage.getHeight)
          val flipRotation = new Rotate(180, Rotate.XAxis)
          ArrowsImage.get.getTransforms.addAll(flipTranslation, flipRotation)
          CurrentDirection = direction
        }

        ChoicesOfSelectedGroup.foreach(c =>
          c.style = s"-fx-background-color: #005246; -fx-text-fill: #c4fcf0; -fx-border-radius: 5"
        )

        // Set text in auxiliary choice labels to help user when picking
        val choiceGroupPane = if getGroupOrientation(from, direction) == Horizontal
          then ListHorBoxes(from - 1)
          else ListVerBoxes(from - 1)

        val auxiliaryChoiceLabels: List[JLabel] = choiceGroupPane.children.map(o=>o.asInstanceOf[JLabel]).toList
        for (i <- ChoicesOfSelectedGroup.indices) {
          ChoicesOfSelectedGroup(i).text = ellipsisIfNecessary(auxiliaryChoiceLabels(i).text.value, LabelMaxChars+11)
        }
      }
      case ChoiceSelected => resetPicking()
      case _ => ()
    }

    if (valid)
      MainButton.fill = ActiveCircleColor
    else {
      resetPicking()
    }

    valid
  }

  private def resetPicking(): Unit = {
    ChoiceGroups.opacity = 1
    ChoicesOfSelectedGroup.foreach(o => o.text = "")
    ArrowsImage.get.visible = false
    MainButton.fill = InactiveCircleColor
  }

  private def getGroupOrientation(boxNumber: Int, direction: MoveDirection): GroupOrientation = {
    (boxNumber, direction) match {
      case (f, Clockwise) if f == 1 || f == 3 => Vertical
      case (f, Counterclockwise) if f == 2 || f == 4 => Vertical
      case _ => Horizontal
    }
  }

  private def ellipsisIfNecessary(s: String, maxChars: Int = LabelMaxChars): String = {
    s.take(maxChars) + (if s.drop(maxChars).nonEmpty then "..." else "")
  }

  /** Set row and column constraints to make grid cells equal */
  private def makeGridCellsEqual(grid: GridPane, cellSize: Int = BlockSize - LabelGridGap): Unit = {
    val c1 = new ColumnConstraints
    val c2 = new ColumnConstraints
    val r1 = new RowConstraints
    val r2 = new RowConstraints

    c1.setMinWidth(cellSize)
    c2.setMinWidth(cellSize)
    r1.setMinHeight(cellSize)
    r2.setMinHeight(cellSize)

    grid.getColumnConstraints.setAll(c1, c2)
    grid.getRowConstraints.setAll(r1, r2)
  }

  private def updateChoiceLabels(): Unit = {
    for(direction <- List(Clockwise, Counterclockwise)) {
      for (i <- 1 to numOfBlocks) {
        val labelStrings = engine.choiceLabels(i, direction)
        val box = if getGroupOrientation(i, direction) == Horizontal then ListHorBoxes(i-1) else ListVerBoxes(i-1)
        val labels = box.children.map(node => node.asInstanceOf[JLabel])

        for(j <- labels.indices)
          labels(j).text = labelStrings(j)
      }
    }
  }
}
