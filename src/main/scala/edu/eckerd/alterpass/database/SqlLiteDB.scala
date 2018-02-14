package edu.eckerd.alterpass.database

import edu.eckerd.alterpass.models._
import edu.eckerd.alterpass.models.Configuration._
import doobie._
import doobie.implicits._
import cats.implicits._
import cats.effect._
import fs2._

trait SqlLiteDB[F[_]]{
  def rateLimitCheck(username: String, time: Long): F[Boolean]
  def writeConnection(username: String, email_code: EmailCode, random: String, time: Long): F[Int]
  def recoveryLink(username: String, url: String, time: Long): F[Option[UserWithEmailCode]]
  def removeRecoveryLink(username: String, url: String): F[Int]
  def removeOlder(time: Long): F[Int]
}

object SqlLiteDB {
  def apply[F[_]](implicit ev: SqlLiteDB[F]): SqlLiteDB[F] = ev

  def impl[F[_]: Async](config: SqlLiteConfig): Stream[F, SqlLiteDB[F]] = for {
    transactor <- Stream(createSqlLiteTransactor[F](config.absolutePath)).covary[F]
    _ <- Stream.eval(createTable.run.transact(transactor))
  } yield new SqlLiteDB[F]{
    override def rateLimitCheck(username: String, time: Long): F[Boolean] = {
      // 1 Day Less Than Current Time
      val minTimeEpoch = time - 900L
      val query = sql"""SELECT
                    username
                    FROM FORGOT_PASSWORD
                    WHERE
                    username = $username
                    AND
                    created >= $minTimeEpoch
                    """.query[String]

      query.to[List].transact(transactor).map(_.headOption.isEmpty)
    }
    override def writeConnection(username: String, email_code: EmailCode, random: String, time: Long): F[Int] = {
      val insert =sql"""INSERT INTO FORGOT_PASSWORD (username, email_code, linkExtension, created)
              VALUES (
                $username,
                $email_code,
                $random,
                $time
              )
          """.update

      insert.run.transact(transactor)
    }

    override def recoveryLink(username: String, url: String, time: Long): F[Option[UserWithEmailCode]] = {
      // 1 Day Less Than Current Time
      val minTimeEpoch = time - 86400L
      val query = sql"""SELECT
                    username,
                    email_code
                    FROM FORGOT_PASSWORD
                    WHERE
                    linkExtension = $url
                    AND
                    username = $username
                    AND
                    created >= $minTimeEpoch
                    """.query[UserWithEmailCode]

      query.option.transact(transactor)
    }

    override def removeRecoveryLink(username: String, url: String): F[Int] = {
      val update = sql"""DELETE FROM FORGOT_PASSWORD
                    WHERE
                    linkExtension = $url
                    AND
                    username = $username
                    """.update

      update.run.transact(transactor)
    }

    override def removeOlder(time: Long): F[Int] = {
      val minTimeEpoch = time - 86400L
      val update : Update0 = sql"""DELETE FROM FORGOT_PASSWORD
                    WHERE
                    created < $minTimeEpoch
                    """.update
      update.run.transact(transactor)
    }
  }

  

  private def createSqlLiteTransactor[F[_]: Async](path: String): Transactor[F] = {

    val newPath = if (path.endsWith("/")) path else s"$path/"
    val sqlliteDriver = "org.sqlite.JDBC"
    val dbName = "alterpass.db"
    val connectionString = s"jdbc:sqlite:$newPath$dbName"


    Transactor.fromDriverManager[F](
      sqlliteDriver,
      connectionString
    )
  }


  private val createTable: Update0 =
    sql"""
       CREATE TABLE IF NOT EXISTS FORGOT_PASSWORD (
        username TEXT,
        email_code TEXT,
        linkExtension TEXT,
        created INTEGER
       )
       """.update

}
