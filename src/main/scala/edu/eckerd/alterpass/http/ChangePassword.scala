package edu.eckerd.alterpass.http

import cats.data.NonEmptyList
import fs2.Task
import org.http4s.CacheDirective.`no-cache`
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Cache-Control`

object ChangePassword {

  val prefix = "/changepw"

  val service = HttpService {

    // Form Page For Change Password, taking Email/Username, Current Password, and New Password
    case req @ GET -> Root =>
      StaticFile.fromResource(s"/pages/$prefix.html", Some(req))
        .map(_.putHeaders())
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .map(Task.now)
        .getOrElse(NotFound())

    // Post
    case req @ POST -> Root =>
      NotImplemented()
  }

}
