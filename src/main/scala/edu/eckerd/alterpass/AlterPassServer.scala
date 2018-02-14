package edu.eckerd.alterpass

import edu.eckerd.alterpass.models.Configuration.ApplicationConfig
import edu.eckerd.alterpass.agingfile.AgingFile
import edu.eckerd.alterpass.database.{OracleDB, SqlLiteDB}
import edu.eckerd.alterpass.google.GoogleAPI
import edu.eckerd.alterpass.http._
import edu.eckerd.alterpass.ldap.Ldap
import edu.eckerd.alterpass.models._
import fs2._
import org.http4s.server.blaze.BlazeBuilder
import edu.eckerd.alterpass.email.EmailService
import scala.concurrent.ExecutionContext
import cats.effect._

object AlterPassServer {

  def stream[F[_]](implicit F: Effect[F], ec: ExecutionContext): Stream[F, StreamApp.ExitCode] = for {
    appConfig <- Stream.eval(Sync[F].delay(pureconfig.loadConfigOrThrow[ApplicationConfig]("edu.eckerd.alterpass")))
    agingFile = AgingFile.impl[F](appConfig.agingFileConfig)
    oracleDb <- OracleDB.impl[F](appConfig.oracleConfig)
    sqlLite <- SqlLiteDB.impl[F](appConfig.sqlLiteConfig)
    emailService <- EmailService.impl[F](appConfig.emailConfig)
    googleApi <- GoogleAPI.impl[F](appConfig.googleConfig)
    ldap <- Ldap.impl[F](appConfig.ldapConfig)

    out <- BlazeBuilder[F]
    .bindHttp(appConfig.httpConfig.port, appConfig.httpConfig.hostname)
    .mountService(StaticSite.service[F])
    .serve
  } yield  out


  // val config: Stream[IO, ApplicationConfig] = 

  // def createTools(applicationConfig: ApplicationConfig): IO[Toolbox] = {
  //   val agingFile = AgingFile(applicationConfig.agingFileConfig.absolutePath)

  //   val ldapT = LdapAdmin.build(
  //     "ldaps",
  //     applicationConfig.ldapConfig.host,
  //     636,
  //     applicationConfig.ldapConfig.baseDN,
  //     applicationConfig.ldapConfig.searchAttribute,
  //     applicationConfig.ldapConfig.user,
  //     applicationConfig.ldapConfig.pass
  //   )
  //   val oracleT = OracleDB.build(
  //     applicationConfig.oracleConfig.host,
  //     applicationConfig.oracleConfig.port,
  //     applicationConfig.oracleConfig.sid,
  //     applicationConfig.oracleConfig.username,
  //     applicationConfig.oracleConfig.pass
  //   )

  //   val sqlLiteT = SqlLiteDB.build(applicationConfig.sqlLiteConfig.absolutePath)

  //   val googleT = GoogleAPI.build(
  //     applicationConfig.googleConfig.serviceAccount,
  //     applicationConfig.googleConfig.administratorAccount,
  //     applicationConfig.googleConfig.credentialFilePath,
  //     applicationConfig.googleConfig.applicationName
  //   )

  //   val blazeBuilder = BlazeBuilder[IO]

  //   val email = Emailer(applicationConfig.emailConfig)

  //   for {
  //     ldap <- ldapT
  //     oracle <- oracleT
  //     google <- googleT
  //     sqlLite <- sqlLiteT
  //   } yield Toolbox(agingFile, ldap, oracle, sqlLite, google, blazeBuilder, email)

  // }

  // def constructServer(toolbox: Toolbox): Stream[IO, StreamApp.ExitCode] = {
  //   val changePasswordService = http.ChangePassword(toolbox)
  //   val forgotPasswordService = http.ForgotPassword(toolbox)
  //   val BlazeBuilder = toolbox.blazeBuilder

  //   BlazeBuilder
  //     .mountService(changePasswordService.service, changePasswordService.prefix)
  //     .mountService(forgotPasswordService.service, forgotPasswordService.prefix)
  //     .mountService(StaticSite.service)
  //     .serve
  // }
}
