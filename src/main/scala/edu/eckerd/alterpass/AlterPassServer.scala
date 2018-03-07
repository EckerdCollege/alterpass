package edu.eckerd.alterpass

import edu.eckerd.alterpass.models.Configuration.ApplicationConfig
import edu.eckerd.alterpass.agingfile.AgingFile
import edu.eckerd.alterpass.database.{OracleDB, SqlLiteDB}
import edu.eckerd.alterpass.google.GoogleAPI
import edu.eckerd.alterpass.http._
import edu.eckerd.alterpass.ldap.Ldap
import edu.eckerd.alterpass.rules.PasswordRules
import fs2._
import org.http4s.server.blaze.BlazeBuilder
import edu.eckerd.alterpass.email.EmailService
import scala.concurrent.ExecutionContext
import cats.effect._
import cats.implicits._
import org.http4s.server.middleware._

object AlterPassServer {

  def stream[F[_]](implicit F: Effect[F], ec: ExecutionContext): Stream[F, StreamApp.ExitCode] = for {
    appConfig <- Stream.eval(Sync[F].delay(pureconfig.loadConfigOrThrow[ApplicationConfig]("edu.eckerd.alterpass")))
    agingFile = AgingFile.impl[F](appConfig.agingFileConfig)
    oracleDb <- OracleDB.impl[F](appConfig.oracleConfig)
    sqlLite <- SqlLiteDB.impl[F](appConfig.sqlLiteConfig)
    emailService <- EmailService.impl[F](appConfig.emailConfig)
    googleApi <- GoogleAPI.impl[F](appConfig.googleConfig)
    ldap <- Ldap.impl[F](appConfig.ldapConfig)

    passRules = PasswordRules.impl[F]

    cp = ChangePassword.impl(F, ldap, agingFile, googleApi, passRules)
    fp = ForgotPassword.impl(F, ldap, agingFile, googleApi, oracleDb, sqlLite, emailService, passRules)
    
    staticService = StaticSite.service[F]
    cpService = ChangePasswordService.service(F, cp)
    fpService = ForgotPasswordService.service(F, fp)

    bareService = cpService <+> fpService <+> staticService

    service = CORS(bareService)

    out <- BlazeBuilder[F]
    .bindHttp(appConfig.httpConfig.port, appConfig.httpConfig.hostname)
    .mountService(service)
    .serve
  } yield  out

}
