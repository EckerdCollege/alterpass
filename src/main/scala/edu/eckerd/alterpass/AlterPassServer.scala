package edu.eckerd.alterpass

import java.util.concurrent.{ExecutorService, Executors}

import edu.eckerd.alterpass.http._
import edu.eckerd.alterpass.ldap.LdapAdmin

import scala.util.Properties.envOrNone
import fs2.{Stream, Task}
import org.http4s.util.StreamApp
import org.http4s.server.blaze.BlazeBuilder


object AlterPassServer extends StreamApp {

  val port : Int              = envOrNone("HTTP_PORT") map (_.toInt) getOrElse 8080
  val ip   : String           = "0.0.0.0"
  val pool : ExecutorService  = Executors.newCachedThreadPool()

  override def stream(args: List[String]): Stream[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(port, ip)
      .mountService(ChangePassword.service, ChangePassword.prefix)
      .mountService(ForgotPassword.service, ForgotPassword.prefix)
      .mountService(StaticSite.service)
      .withServiceExecutor(pool)
      .serve
  }

}
