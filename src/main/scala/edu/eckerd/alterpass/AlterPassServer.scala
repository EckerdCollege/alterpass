package edu.eckerd.alterpass

import java.util.concurrent.{ExecutorService, Executors}

import edu.eckerd.alterpass.Configuration.ApplicationConfig
import edu.eckerd.alterpass.agingfile.AgingFile
import edu.eckerd.alterpass.database.{OracleDB, SqlLiteDB}
import edu.eckerd.alterpass.google.GoogleAPI
import edu.eckerd.alterpass.http._
import edu.eckerd.alterpass.ldap.LdapAdmin
import edu.eckerd.alterpass.models.Toolbox
import fs2.{Strategy, Stream, Task}
import org.http4s.util.StreamApp
import org.http4s.server.blaze.BlazeBuilder
import Configuration.loadAllFromEnv
import edu.eckerd.alterpass.email.Emailer

object AlterPassServer extends StreamApp {

  implicit val strategy = Strategy.fromExecutionContext(scala.concurrent.ExecutionContext.global)

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    config
      .flatMap(c => Stream.eval(createTools(c)))
      .flatMap(constructServer)
  }

  val config: Stream[Task, ApplicationConfig] = Stream.eval(loadAllFromEnv())

  def createTools(applicationConfig: ApplicationConfig)(implicit strategy: Strategy): Task[Toolbox] = {
    val agingFile = AgingFile(applicationConfig.agingFileConfig.absolutePath)

    val ldapT = LdapAdmin.build(
      "ldaps",
      applicationConfig.ldapConfig.host,
      636,
      applicationConfig.ldapConfig.baseDN,
      applicationConfig.ldapConfig.searchAttribute,
      applicationConfig.ldapConfig.user,
      applicationConfig.ldapConfig.pass
    )
    val oracleT = OracleDB.build(
      applicationConfig.oracleConfig.host,
      applicationConfig.oracleConfig.port,
      applicationConfig.oracleConfig.sid,
      applicationConfig.oracleConfig.username,
      applicationConfig.oracleConfig.pass
    )

    val sqlLiteT = SqlLiteDB.build(applicationConfig.sqlLiteConfig.absolutePath)

    val googleT = GoogleAPI.build(
      applicationConfig.googleConfig.domain,
      applicationConfig.googleConfig.serviceAccount,
      applicationConfig.googleConfig.administratorAccount,
      applicationConfig.googleConfig.credentialFilePath,
      applicationConfig.googleConfig.applicationName
    )

    val blazeBuilder = BlazeBuilder.bindHttp(applicationConfig.httpConfig.port, applicationConfig.httpConfig.hostname)

    val email = Emailer(applicationConfig.emailConfig)

    for {
      ldap <- ldapT
      oracle <- oracleT
      google <- googleT
      sqlLite <- sqlLiteT
    } yield Toolbox(agingFile, ldap, oracle, sqlLite, google, blazeBuilder, email)

  }

  def constructServer(toolbox: Toolbox): Stream[Task, Nothing] = {
    val changePasswordService = http.ChangePassword(toolbox)
    val forgotPasswordService = http.ForgotPassword(toolbox)
    val BlazeBuilder = toolbox.blazeBuilder

    BlazeBuilder
      .mountService(changePasswordService.service, changePasswordService.prefix)
      .mountService(forgotPasswordService.service, forgotPasswordService.prefix)
      .mountService(StaticSite.service)
      .serve
  }
}
