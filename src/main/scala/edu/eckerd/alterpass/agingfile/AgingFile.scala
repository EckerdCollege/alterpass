package edu.eckerd.alterpass.agingfile

import fs2._
import fs2.io._
import java.nio.file.StandardOpenOption._
import java.nio.file.FileSystems
import cats.effect.IO

case class AgingFile(filePath: String) {

  def writeUsernamePass(user: String, pass: String): IO[Unit] = {
    writeUsernamePass(
      user,
      pass,
      file.writeAll(FileSystems.getDefault.getPath(filePath), List(CREATE, WRITE, APPEND))
    )
  }

  def writeUsernamePass(user: String, pass: String, sink: Sink[IO, Byte]): IO[Unit] = {
    val combineText = s"$user:$pass\n"
    Stream(combineText)
      .covary[IO]
      .through(text.utf8Encode)
      .to(sink)
      .run
  }

}
