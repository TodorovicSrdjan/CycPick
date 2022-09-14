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

import engine.choice.Choice

import org.json4s.*
import org.json4s.jackson.Serialization.{read, writePretty}
import org.json4s.{CustomSerializer, Extraction, JArray, JField, JInt, JObject}

class ChoiceGroupSerializer extends CustomSerializer[ChoiceGroup](format => ( {
  case JObject(
  JField("groupNumber", JInt(i)) :: JField("choices", JArray(choices)) :: Nil) =>
    new ChoiceGroup(i.toInt, choices.map(c => c.extract[Choice]))
}, {
  case x: ChoiceGroup =>
    JObject(
      JField("groupNumber", JInt(x.groupNumber)) ::
        JField("choices", JArray(x.choices.map(c => Extraction.decompose(c)))) ::
        Nil
    )
}
))
