package edu.eckerd.alterpass.models

import cats.data._
import cats.effect._
import io.circe._
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.circe._

final case class ErrorResponse(
  code: Int,
  errors: NonEmptyList[String]
)

object ErrorResponse{
  implicit val errorResponseEncoder : Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  implicit def errorResponseEntityEncoder[F[_]: Sync]: EntityEncoder[F, ErrorResponse] = jsonEncoderOf[F, ErrorResponse]
}