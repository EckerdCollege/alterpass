package edu.eckerd.alterpass

import fs2.{Stream,StreamApp}
import cats.effect.IO
import scala.concurrent.ExecutionContext.Implicits.global

object Main extends StreamApp[IO]{
    def stream(args: List[String], requestShutdown: IO[Unit]): Stream[IO, StreamApp.ExitCode] = 
        AlterPassServer.stream[IO]
}