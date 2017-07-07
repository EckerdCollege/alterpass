package edu.eckerd.alterpass.database

import fs2.Task
import fs2.interop.cats._
import doobie.imports._
import doobie.hikari.imports._
import cats.data._
import edu.eckerd.alterpass.errors.OracleError
import cats.implicits._
import cats._


case class OracleDB(host: String, port: Int, sid: String, hikariTransactor: HikariTransactor[Task]) {



  def getPersonalEmails(username: String): Task[NonEmptyList[String]] = {
    val newUserName = if (username.endsWith("@eckerd.edu")) username else s"$username@eckerd.edu"


    val q =sql"""SELECT gPersonal.GOREMAL_EMAIL_ADDRESS as PERSONAL_EMAIL
        FROM GOREMAL gSchool
        INNER JOIN
          GOREMAL gPersonal
            ON gSchool.GOREMAL_PIDM = gPersonal.GOREMAL_PIDM
        WHERE
          gSchool.GOREMAL_EMAL_CODE in ('CA', 'CAS', 'ECA')
        AND
          gSchool.GOREMAL_STATUS_IND = 'A'
        AND
          gPersonal.GOREMAL_EMAL_CODE = 'PR'
        AND
          gSchool.GOREMAL_EMAIL_ADDRESS= $newUserName
      """.query[String]

      q.nel.transact(hikariTransactor)
  }


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
