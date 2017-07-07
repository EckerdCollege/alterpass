package edu.eckerd.alterpass.models

import io.circe.generic._

@JsonCodec case class ChangePasswordReceived(
                                    username: String,
                                    oldPass: String,
                                    newPass: String
                                    )
