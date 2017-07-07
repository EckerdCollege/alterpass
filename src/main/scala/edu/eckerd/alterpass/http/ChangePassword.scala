package edu.eckerd.alterpass.http

import cats.data.NonEmptyList
import edu.eckerd.alterpass.models.{ChangePasswordReceived, Toolbox}
import fs2.{Strategy, Task}
import org.http4s.CacheDirective.`no-cache`
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers.`Cache-Control`
import org.http4s.circe._
import fs2.interop.cats._
import cats.implicits._

case class ChangePassword(toolbox: Toolbox)(implicit strategy: Strategy) {

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
      for {
        cpw <- req.as(jsonOf[ChangePasswordReceived])
        bool <- toolbox.ldapAdmin.checkBind(cpw.username, cpw.oldPass)
        resp <- if (bool){
          toolbox.agingFile.writeUsernamePass(cpw.username, cpw.newPass) >>
          toolbox.ldapAdmin.setUserPassword(cpw.username, cpw.newPass) >>
          toolbox.googleAPI.changePassword(cpw.username, cpw.newPass) >>
            Created()
        } else {
          BadRequest()
        }

      } yield resp
  }

}
