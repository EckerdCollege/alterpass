package edu.eckerd.alterpass.database

import fs2.Task
import fs2.interop.cats._
import doobie.imports._
import doobie.hikari.imports._
import cats.implicits._
import cats._


case class OracleDB(host: String, port: Int, sid: String, hikariTransactor: HikariTransactor[Task]) {



}

object OracleDB {
  def createOracleTransactor(
                              host: String,
                              port: Int,
                              sid: String,
                              username: String,
                              password: String
                            ): Task[HikariTransactor[Task]] = {

    val oracle_driver = "oracle.jdbc.driver.OracleDriver"
    val oracle_connection_string = s"jdbc:oracle:thin:@//$host:$port/$sid"
    HikariTransactor[Task](oracle_driver,
      oracle_connection_string,
      username,
      password)
  }

  def build(
             host: String,
             port: Int,
             sid: String,
             username: String,
             password: String
           ): Task[OracleDB] = {
    createOracleTransactor(host, port, sid, username, password)
      .map(hikariTransactor => OracleDB(host, port, sid, hikariTransactor))
  }

}
