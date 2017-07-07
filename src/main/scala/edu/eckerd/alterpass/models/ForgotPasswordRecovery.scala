package edu.eckerd.alterpass.models

import io.circe.generic.JsonCodec

@JsonCodec case class ForgotPasswordRecovery(
                                 username: String,
                                 newPass: String
                                 )
