package edu.eckerd.alterpass.models

import io.circe.generic._

@JsonCodec case class User(
                            username: String,
                            password: String
                          )
