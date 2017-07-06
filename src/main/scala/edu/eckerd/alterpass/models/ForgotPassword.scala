package edu.eckerd.alterpass.models

import io.circe.generic._

@JsonCodec case class ForgotPassword(
                                      username: String,
                                      uuid: String
                                    )
