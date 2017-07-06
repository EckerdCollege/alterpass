package edu.eckerd.alterpass.models

import io.circe.generic._

@JsonCodec case class ChangePassword(
                                    userName: String,
                                    oldPass: String,
                                    newPass: String
                                    )
