package edu.eckerd.alterpass.agingfile

import fs2._
import fs2.io._
import java.nio.file.StandardOpenOption._
import java.nio.file.FileSystems
import cats.implicits._
import cats.effect._
import edu.eckerd.alterpass.models.Configuration.AgingFileConfig

trait AgingFile[F[_]]{
  def writeUsernamePass(user: String, pass: String): F[Unit]
}

object AgingFile{
  def impl[F[_]: Effect](config: AgingFileConfig): AgingFile[F] = new AgingFile[F]{
    override def writeUsernamePass(user: String, pass: String): F[Unit] = 
      Stream(show"${user}:${pass}\n")
        .covary[F]
        .through(text.utf8Encode)
        .to(file.writeAll[F](FileSystems.getDefault.getPath(config.absolutePath), List(CREATE, WRITE, APPEND)))
        .compile
        .drain
  }
}
