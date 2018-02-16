package edu.eckerd.alterpass.errors

import io.circe.Encoder
import io.circe.Decoder
import io.circe._
import io.circe.generic._
import cats.implicits._
import cats.kernel.Eq

@JsonCodec
case class AlterPassError(message: String)
