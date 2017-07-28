package edu.eckerd.alterpass.models

import cats.data.NonEmptyList
import io.circe.generic.JsonCodec

@JsonCodec case class ForgotPasswordReturn(emails: List[String])
