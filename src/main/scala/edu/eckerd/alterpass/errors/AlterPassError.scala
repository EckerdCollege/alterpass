package edu.eckerd.alterpass.errors

import io.circe.Encoder
import io.circe.Decoder
import io.circe._
import cats.implicits._
import cats.kernel.Eq

sealed trait AlterPassError {
  val errorType: String
  val message: String
}

object AlterPassError {
  implicit val alterPassErrorEncoder : Encoder[AlterPassError] = Encoder.instance( alterPassError =>
    Json.fromJsonObject(
      JsonObject.fromIterable(
        List(
          ("errorType" -> Json.fromString(alterPassError.errorType)),
          ("message" -> Json.fromString(alterPassError.message))
        )
      )
    )
  )

  implicit val alterPassDecoder : Decoder[AlterPassError] = Decoder.instance { hCursor =>
    val eType = hCursor.get[String]("errorType")
    val message = hCursor.get[String]("message")

    for {
      e <- eType
      m <- message
    } yield GenericError(e, m)
  }

  implicit val alterPassEq : Eq[AlterPassError] = Eq.instance{ case (a, b) =>
      a.errorType === b.errorType && a.message === b.message
  }

}

case class OracleError(message: String) extends AlterPassError {
  val errorType = "Oracle Error"
}


case class GenericError(errorType: String, message: String) extends AlterPassError
