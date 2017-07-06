package edu.eckerd.alterpass.models

import io.circe.generic.JsonCodec

@JsonCodec case class ForgotPasswordReturn(username: String, emails: List[String])
