package edu.eckerd.alterpass.agingfile

import fs2._
import fs2.io._
import java.nio.file.StandardOpenOption._
import java.nio.file.FileSystems


case class AgingFile(filePath: String) {

  def writeUsernamePass(user: String, pass: String)(implicit strategy: Strategy): Task[Unit] = {
    writeUsernamePass(
      user,
      pass,
      file.writeAll(FileSystems.getDefault.getPath(filePath), List(CREATE, WRITE, APPEND))
    )(strategy)
  }

  def writeUsernamePass(user: String, pass: String, sink: Sink[Task, Byte])(implicit strategy: Strategy): Task[Unit] = {
    val combineText = s"$user;$pass\n"
    Stream(combineText)
      .covary[Task]
      .through(text.utf8Encode)
      .to(sink)
      .run
  }

}
