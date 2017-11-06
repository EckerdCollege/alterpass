package edu.eckerd.alterpass.models

import io.circe.generic.JsonCodec

@JsonCodec case class ForgotPasswordReturn(emails: List[String])
