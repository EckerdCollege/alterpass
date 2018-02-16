package edu.eckerd.alterpass.models

import enumeratum._
import doobie._

sealed trait EmailCode extends EnumEntry
object EmailCode extends Enum[EmailCode] {
  val values = findValues
  case object CA extends EmailCode
  case object CAS extends EmailCode
  case object ECA extends EmailCode

  implicit val EmailCodeMeta : Meta[EmailCode] = 
    Meta[String].xmap(EmailCode.withName(_), _.entryName)
}