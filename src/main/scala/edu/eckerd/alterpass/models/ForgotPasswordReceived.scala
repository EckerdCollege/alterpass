package edu.eckerd.alterpass.models

import io.circe.generic._

@JsonCodec case class ForgotPasswordReceived(username: String)
