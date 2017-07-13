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
import org.http4s.server.middleware.CORS

case class ChangePassword(toolbox: Toolbox)(implicit strategy: Strategy) {

  val prefix = "/changepw"

  val service = CORS {
    HttpService {

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
            val ldapUserName = cpw.username.replaceAll("@eckerd.edu", "")
            val googleUserName = if (cpw.username.endsWith("@eckerd.edu")) cpw.username else s"${cpw.username}@eckerd.edu"

            for {
              agingFile <- toolbox.agingFile.writeUsernamePass(ldapUserName, cpw.newPass)
              setPass <- toolbox.ldapAdmin.setUserPassword(ldapUserName, cpw.newPass)
              google <- toolbox.googleAPI.changePassword(googleUserName, cpw.newPass)
              resp <- Created(s"Ldap: ${setPass.toString}, Google: ${google}")
            } yield resp
          } else {
            BadRequest(bool.toString)
          }

        } yield resp
    }
  }


}
