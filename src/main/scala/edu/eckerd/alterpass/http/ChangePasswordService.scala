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
import org.http4s.CacheDirective.`no-cache`
import org.http4s.headers.`Cache-Control`
import _root_.io.circe._

object ChangePasswordService{

  private val logger = org.log4s.getLogger
  
  
  def service[F[_]](implicit F: Sync[F], C: ChangePassword[F]): HttpService[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    
    HttpService[F]{
      case req @ GET -> Root / "changepw" => 
        StaticFile.fromResource("/pages/changepw.html", Some(req))
          .map(_.putHeaders())
          .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
          .getOrElseF(NotFound())
      case req @ POST -> Root / "changepw" =>
        req.decodeJson[ChangePasswordReceived]
        .flatMap(cpw => 
          ChangePassword[F].changePassword(cpw.username, cpw.oldPass, cpw.newPass)
            .handleErrorWith(e => 
              Sync[F].delay(logger.error(e)(s"Error With Change Password for ${cpw.username}")) *> 
              Sync[F].raiseError[Unit](e)
            )
        )
        .flatMap(_ => Created())
        .handleErrorWith{
          case Ldap.BindFailure => BadRequest()
          case DecodingFailure(_,_) => BadRequest(ErrorResponse(400, NonEmptyList.of("Invalid Json")))
          case PasswordRules.ValidationFailure(nel) => BadRequest(ErrorResponse(400, nel))
          case _ => InternalServerError()
        }

    }
  }
}