package engine.choice

import engine.choice.ChoiceType.ChoiceType

import org.json4s.jackson.Serialization.{read, writePretty}
import org.json4s.{CustomSerializer, JObject, JString, JField}

class ChoiceTypeSerializer extends CustomSerializer[ChoiceType](format => ( {
  case JString(ct) =>
    val choiceType = ChoiceType.withName(ct)
    choiceType match {
      case ChoiceType.TextWriter => ChoiceType.TextWriter
      case ChoiceType.OneTimeUse => ChoiceType.OneTimeUse
      case ChoiceType.ChoiceWithSubchoices => ChoiceType.ChoiceWithSubchoices
      case x => throw new Exception(s"Invalid value $x for choice type")
    }
}, {
  case choiceType: ChoiceType => JString(choiceType.toString)
}
))

