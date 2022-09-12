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

import engine.NodeType

object NodeType extends Enumeration {
  type NodeType = Value
  val OneTimeUse, Regular = Value
}

class Node(val label: String, val nodeType: NodeType.NodeType, val parent: Node, val isLeaf: Boolean = true) {
  var content = null
  var options = null
    /*
      def this(label: String, nodeType: NodeType, parent: Node) {
        this(label, nodeType, parent)if (!isLeaf) {
      options = createNodeOptions()
    }
      }*/


  private def createNodeOptions(): List[Node] = {
    List()
  }

  def unload(): Unit = {

  }
}
