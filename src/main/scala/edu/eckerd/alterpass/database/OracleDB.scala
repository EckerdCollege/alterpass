package edu.eckerd.alterpass.database

import edu.eckerd.alterpass.models.Configuration.OracleConfig
import edu.eckerd.alterpass.models.{Email, EmailCode}
import cats._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.hikari._
import cats.effect._
import enumeratum._
import fs2._

trait OracleDB[F[_]]{
  def getPersonalEmails(username: String): F[List[Email]]
}

object OracleDB {

  def apply[F[_]](implicit ev: OracleDB[F]): OracleDB[F] = ev

  private val logger = org.log4s.getLogger
  
  def impl[F[_]: Effect](oracleConfig: OracleConfig): Stream[F, OracleDB[F]] = {
    if (oracleConfig.enabled){
      val oracle_driver = "oracle.jdbc.driver.OracleDriver"
      val jdbcUrl = s"jdbc:oracle:thin:@//${oracleConfig.host}:${oracleConfig.port}/${oracleConfig.sid}"
      for {
        _ <- Stream.eval(
          Sync[F].delay(logger.info(s"Attempting to Connect to Database - '${jdbcUrl}' as '${oracleConfig.username}'"))
        )
        transactor <- HikariTransactor.stream[F](
          oracle_driver,
          jdbcUrl, 
          oracleConfig.username, 
          oracleConfig.pass
        )
        _ <- Stream.eval(
          Sync[F].delay(logger.info(s"transactor for Oracle connected to '${jdbcUrl}' as '${oracleConfig.username}'"))
        )

      } yield new OracleDB[F]{
        override def getPersonalEmails(username: String): F[List[Email]] = getPersonalEmailsP[F](username)(Monad[F], transactor)
      } 
    
    } else {
      new OracleDB[F]{
        override def getPersonalEmails(username: String): F[List[Email]] = 
        Sync[F].delay(logger.info("Oracle DB Disabled - Generating Fake Email"))
          .as(List(Email("testingEmail@eckerd.edu", EmailCode.CA)))
      }.pure[Stream[F, ?]]

    }
  }

  private def getPersonalEmailsP[F[_]](username: String)(implicit F: Monad[F], T: Transactor[F]): F[List[Email]] = {
    val newUserName = if (username.endsWith("@eckerd.edu")) username else s"$username@eckerd.edu"

    val q =sql"""SELECT
             gPersonal.GOREMAL_EMAIL_ADDRESS as PERSONAL_EMAIL,
             gSchool.GOREMAL_EMAL_CODE as EMAIL_CODE
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
          gPersonal.GOREMAL_STATUS_IND = 'A'
        AND
          gSchool.GOREMAL_EMAIL_ADDRESS= $newUserName
      """.query[Email]

      q.to[List].transact(T)
  }



}

// case class OracleDB(host: String, port: Int, sid: String, hikariTransactor: HikariTransactor[IO]) {




// }

// object OracleDB {
 

//   def build(
//              host: String,
//              port: Int,
//              sid: String,
//              username: String,
//              password: String
//            ): IO[OracleDB] = {
//     createOracleTransactor(host, port, sid, username, password)
//       .map(hikariTransactor => OracleDB(host, port, sid, hikariTransactor))
//   }

// }
