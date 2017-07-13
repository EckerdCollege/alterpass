package edu.eckerd.alterpass.http

import java.time.Instant

import cats.data.NonEmptyList
import edu.eckerd.alterpass.models._
import fs2._
import cats.implicits._
import fs2.interop.cats._
import org.http4s.CacheDirective.`no-cache`
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.headers.`Cache-Control`
import _root_.io.circe.syntax._
import edu.eckerd.alterpass.errors.{AlterPassError, GenericError}
import org.http4s.server.middleware.CORS

import scala.annotation.tailrec

case class ForgotPassword(tools: Toolbox)(implicit strategy: Strategy) {
  import ForgotPassword._

  // Prefix Will Be Prepended to All Roots of this Service
  val prefix = "/forgotpw"

  val service = CORS {
    HttpService {

      // Page Displaying Form for Email Address to Reset
      case req @ GET -> Root =>
        StaticFile.fromResource(s"/pages/$prefix.html", Some(req))
          .map(_.putHeaders())
          .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
          .map(Task.now)
          .getOrElse(NotFound())

      // Post Location Taking Email Address to Have Password Reset
      case req @ POST -> Root =>
        forgotPassWordReceivedRandom(tools, req)

      // Returns Form Taking Username, Date of Birth, and New Password
      case req @ GET -> Root / randomExtension =>
        StaticFile.fromResource(s"/pages/recovery.html", Some(req))
          .map(_.putHeaders())
          .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
          .map(Task.now)
          .getOrElse(NotFound())

      // Post Location for the return page
      case req @ POST -> Root / randomExtension =>
        forgotPasswordRecoveryWithTime(tools, req, randomExtension)

    }
  }



}

object ForgotPassword {

  def forgotPassWordReceived(
                              tools: Toolbox,
                              request: Request,
                              f: () => String,
                              g: () => Long
                            )(implicit strategy: Strategy): Task[Response] = {
    val rand = f()
    for {
      fp <- request.as(jsonOf[ForgotPasswordReceived])
      bool <- tools.sqlLiteDB.rateLimitCheck(fp.username, g())
      resp <- if (bool) {
        for {
        personalEmails <- tools.oracleDB.getPersonalEmails(fp.username)
        rem <- tools.sqlLiteDB.removeOlder(g())
        att <- tools.sqlLiteDB.writeConnection(fp.username, rand, g()).attempt
        resp <- {
          att.fold(
            e =>
              NotFound(
                GenericError(
                  errorType = "Generic",
                  message = s"Problem Receiving Information - ${e.getMessage}"
                ).asInstanceOf[AlterPassError].asJson
              ),
            i =>
              tools.email.sendNotificationEmail(personalEmails, rand) >>
                Created(ForgotPasswordReturn(personalEmails.map(concealEmail)).asJson)
          )
        }
        } yield resp
      } else {
        BadRequest(GenericError("Generic", "RateLimit exceeded").asInstanceOf[AlterPassError].asJson)
      }
    } yield resp
  }

  def concealEmail(email: String): String = {
    def obscure(text: String) = "*" * text.length
    val validEmail = "(.*)@(.*)".r
    val shortMailbox = "(.{1,2})".r
    val longMailbox = "(.)(.)(.*)".r

    email match {
      case validEmail(shortMailbox(m), domain) =>
        s"${obscure(m)}@$domain"
      case validEmail(longMailbox(first, second, middle), domain) =>
        s"$first$second${obscure(middle)}@$domain"
      case other => obscure(other)
    }


  }

  def forgotPassWordReceivedRandom(
                                    tools: Toolbox,
                                    request: Request
                                  )(implicit strategy: Strategy): Task[Response] = {
    val f = () => randomAlphaNumeric(40)
    val g = () => Instant.now().getEpochSecond

    forgotPassWordReceived(tools, request, f, g)
  }







  def forgotPasswordRecovery(
                            tools: Toolbox,
                            request: Request,
                            url: String,
                            g: () => Long
                            )(implicit strategy: Strategy): Task[Response] = {
    for {
      fpr <- request.as(jsonOf[ForgotPasswordRecovery])
      rem <- tools.sqlLiteDB.removeOlder(g())
      bool <- tools.sqlLiteDB.recoveryLink(fpr.username, url, g())
      resp <- {
        if (bool)
          resetAllPasswords(tools, fpr) >>
          tools.sqlLiteDB.removeRecoveryLink(fpr.username, url) >>
            Created(ForgotPasswordReceived(fpr.username).asJson)
        else
          BadRequest()
      }

    } yield resp
  }

  def resetAllPasswords(tools: Toolbox, fpr: ForgotPasswordRecovery)(implicit strategy: Strategy): Task[Unit] = {
    val ldapUserName = fpr.username.replaceAll("@eckerd.edu", "")
    val googleUserName = if (fpr.username.endsWith("@eckerd.edu")) fpr.username else s"${fpr.username}@eckerd.edu"

    val resetLDAP = tools.ldapAdmin.setUserPassword(ldapUserName, fpr.newPass)
    val resetGoogle = tools.googleAPI.changePassword(googleUserName, fpr.newPass)
    val writeFile = tools.agingFile.writeUsernamePass(ldapUserName, fpr.newPass)

    writeFile >> resetLDAP >> resetGoogle >> Task.now(())
  }


  def forgotPasswordRecoveryWithTime(
                                      tools: Toolbox,
                                      request: Request,
                                      url: String
                                    )(implicit strategy: Strategy): Task[Response] = {

    val g = () => Instant.now().getEpochSecond
    forgotPasswordRecovery(tools, request, url, g)
  }


  def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = scala.util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  def randomAlphaNumeric(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('1' to '9')
    randomStringFromCharList(length, chars)
  }







}
