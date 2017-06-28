package edu.eckerd.alterpass.http

import cats.data.NonEmptyList
import fs2.Task
import org.http4s.CacheDirective.`no-cache`
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Cache-Control`

object ForgotPassword {

  // Prefix Will Be Prepended to All Roots of this Service
  val prefix = "/forgotpw"

  val service = HttpService {

    // Page Displaying Form for Email Address to Reset
    case req @ GET -> Root =>
      StaticFile.fromResource(s"/pages/$prefix.html", Some(req))
        .map(_.putHeaders())
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .map(Task.now)
        .getOrElse(NotFound())

    // Post Location Taking Email Address to Have Password Reset
    case req @ POST -> Root =>
      NotImplemented()

    // Returns Form Taking Username, Date of Birth, and New Password
    case req @ GET -> Root / randomExtension =>
      StaticFile.fromResource(s"/pages/recovery.html", Some(req))
        .map(_.putHeaders())
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .map(Task.now)
        .getOrElse(NotFound())

    // Post Location for the return page
    case req @ POST -> Root / randomExtension =>

      NotImplemented()

  }

}
