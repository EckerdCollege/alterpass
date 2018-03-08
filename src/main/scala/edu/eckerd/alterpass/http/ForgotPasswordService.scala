package edu.eckerd.alterpass.http

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._ 
import edu.eckerd.alterpass.models._
import edu.eckerd.alterpass.ldap._
import edu.eckerd.alterpass.rules._
import edu.eckerd.alterpass.database._
import org.http4s.CacheDirective.`no-cache`
import org.http4s.headers.`Cache-Control`
import _root_.io.circe._
import _root_.io.circe.syntax._

object ForgotPasswordService {

  private val logger = org.log4s.getLogger

  def service[F[_]](implicit F: Effect[F], FP: ForgotPassword[F]): HttpService[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._

    HttpService[F]{
      // Page Displaying Form for Email Address to Reset
      case req @ GET -> Root / "forgotpw" =>
        StaticFile.fromResource(s"/pages/forgotpw.html", Some(req))
          .map(_.putHeaders())
          .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
          .getOrElseF(NotFound())

      // Post Location Taking Email Address to Have Password Reset
      case req @ POST -> Root / "forgotpw" =>
        req.decodeJson[ForgotPasswordReceived]
        .flatMap{fpr => 
          ForgotPassword[F].initiatePasswordReset(fpr.username)
            .handleErrorWith(e => 
              Sync[F].delay(logger.error(e)(s"Error in ForgotPassword Initiation ${fpr}")) *> 
              Sync[F].raiseError[ForgotPasswordReturn](e)
            )
        }
        .flatMap(fpr => Created(fpr.asJson))
        .handleErrorWith{
          case SqlLiteDB.RateLimiteCheckFailed => BadRequest()
          case OracleDB.NoPersonalEmailsFound => BadRequest()
          case _ => InternalServerError()
        }

      // Returns Form Taking Username, Date of Birth, and New Password
      case req @ GET -> Root / "forgotpw" / randomExtension =>
        StaticFile.fromResource(s"/pages/recovery.html", Some(req))
          .map(_.putHeaders())
          .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
          .getOrElseF(NotFound())

      // Post Location for the return page
      case req @ POST -> Root / "forgotpw" / randomExtension =>
        req.decodeJson[ForgotPasswordRecovery]
        .flatMap{fpr => 
          ForgotPassword[F].resetPassword(fpr.username, fpr.newPass, randomExtension)
            .handleErrorWith{
              case e@PasswordRules.ValidationFailure(nel) =>
                Sync[F].delay(logger.info(s"Invalid Password Provided By User: ${fpr.username} - Rules Broken: ${nel}")) *>
                  Sync[F].raiseError[Unit](e)
              case e => 
              Sync[F].delay(logger.error(e)(s"Error With Forgot Password Recovery for Username: ${fpr.username}")) *>
                Sync[F].raiseError[Unit](e)
            }
        }
        .flatMap(_ => Created())
        .handleErrorWith{
          case SqlLiteDB.MissingRecoveryLink => BadRequest()
          case PasswordRules.ValidationFailure(nel) => BadRequest(ErrorResponse(400, nel))
          case _ => InternalServerError()
        }
    }
  }
}