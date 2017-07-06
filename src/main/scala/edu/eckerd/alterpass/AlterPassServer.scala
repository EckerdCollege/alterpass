package edu.eckerd.alterpass

import java.util.concurrent.{ExecutorService, Executors}

import edu.eckerd.alterpass.Configuration.ApplicationConfig
import edu.eckerd.alterpass.agingfile.AgingFile
import edu.eckerd.alterpass.database.{OracleDB, SqlLiteDB}
import edu.eckerd.alterpass.google.GoogleAPI
import edu.eckerd.alterpass.http._
import edu.eckerd.alterpass.ldap.LdapAdmin

import scala.util.Properties.envOrNone
import fs2.{Stream, Task, Strategy}
import org.http4s.util.StreamApp
import org.http4s.server.blaze.BlazeBuilder


object AlterPassServer extends StreamApp {

//  val port : Int              = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080
//  val ip   : String           = "0.0.0.0"
  val pool : ExecutorService  = Executors.newCachedThreadPool()
  implicit val strategy = Strategy.fromExecutionContext(scala.concurrent.ExecutionContext.global)

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    config.flatMap( c =>
      Stream.eval(createTools(c))
    ).flatMap( tools =>
      Stream.eval{Task{println(tools); tools}}
    ).flatMap(tools =>
      tools._6
        .mountService(ChangePassword.service, ChangePassword.prefix)
        .mountService(ForgotPassword.service, ForgotPassword.prefix)
        .mountService(StaticSite.service)
        .withServiceExecutor(pool)
        .serve
    )

  }

  val config: Stream[Task, ApplicationConfig] = Stream.eval(Configuration.loadAllFromEnv())

  def createTools(applicationConfig: ApplicationConfig): Task[(AgingFile, LdapAdmin, OracleDB, SqlLiteDB, GoogleAPI, BlazeBuilder)] = {
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

    val sqlLite = SqlLiteDB(applicationConfig.sqlLiteConfig.absolutePath)

    val googleT = GoogleAPI.build(
      applicationConfig.googleConfig.domain,
      applicationConfig.googleConfig.serviceAccount,
      applicationConfig.googleConfig.administratorAccount,
      applicationConfig.googleConfig.credentialFilePath,
      applicationConfig.googleConfig.applicationName
    )

    val blazeBuilder = BlazeBuilder.bindHttp(applicationConfig.httpConfig.port, applicationConfig.httpConfig.hostname)

    for {
      ldap <- ldapT
      oracle <- oracleT
      google <- googleT
    } yield (agingFile, ldap, oracle, sqlLite, google, blazeBuilder)




  }

//  BlazeBuilder
//    .bindHttp(port, ip)
//    .mountService(ChangePassword.service, ChangePassword.prefix)
//    .mountService(ForgotPassword.service, ForgotPassword.prefix)
//    .mountService(StaticSite.service)
//    .withServiceExecutor(pool)
//    .serve
}
