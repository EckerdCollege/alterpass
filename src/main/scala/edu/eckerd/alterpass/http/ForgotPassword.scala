package edu.eckerd.alterpass.http

import cats.effect._
import edu.eckerd.alterpass.agingfile._
import edu.eckerd.alterpass.database._
import edu.eckerd.alterpass.email._
import edu.eckerd.alterpass.ldap._
import edu.eckerd.alterpass.google._
import edu.eckerd.alterpass.models._
import edu.eckerd.alterpass.rules._
import cats._
import cats.implicits._
import java.time.Instant

trait ForgotPassword[F[_]]{
  def initiatePasswordReset(username: String): F[ForgotPasswordReturn]
  def resetPassword(userName: String, newPass: String, extension: String): F[Unit]
}

object ForgotPassword {
  def apply[F[_]](implicit ev: ForgotPassword[F]): ForgotPassword[F] = ev

  private val logger = org.log4s.getLogger

  def impl[F[_]](
    implicit F: Effect[F], 
    L: Ldap[F], 
    A: AgingFile[F],
    G: GoogleAPI[F],
    O: OracleDB[F],
    S: SqlLiteDB[F],
    E: EmailService[F],
    P: PasswordRules[F]
  ): ForgotPassword[F] = new ForgotPassword[F] {

    override def initiatePasswordReset(username: String): F[ForgotPasswordReturn] = for {
      now <- Sync[F].delay(Instant.now().getEpochSecond)
      _ <- SqlLiteDB[F].rateLimitCheck(username, now)
      _ <- SqlLiteDB[F].removeOlder(now)
      personalEmails <-OracleDB[F].getPersonalEmails(username)
      random <- Sync[F].delay(randomAlphaNumeric(40))
      _ <- EmailService[F].sendNotificationEmail(personalEmails.toList.map(_.emailAddress), random)
      _ <- SqlLiteDB[F].writeConnection(username, personalEmails.head.emailCode, random, now)
      concealedAddresses = personalEmails.toList.map(_.emailAddress).map(concealEmail)
      _ <- Sync[F].delay(logger.info(s"Forgot Password Reset Initiated For User: ${username}"))
    } yield ForgotPasswordReturn(concealedAddresses)

    override def resetPassword(userName: String, newPass: String, extension: String): F[Unit] = {
      val ldapUserName = userName.replaceAll("@eckerd.edu", "")
      val googleUserName = if (userName.endsWith("@eckerd.edu")) userName else s"${userName}@eckerd.edu"

      for {
        _ <- PasswordRules[F].validate(newPass)
        now <- Sync[F].delay(Instant.now().getEpochSecond) 
        _ <- SqlLiteDB[F].removeOlder(now)
        user <- SqlLiteDB[F].recoveryLink(userName, extension, now)
        _ <- if (user.emailCode != EmailCode.ECA) Ldap[F].setUserPassword(ldapUserName, newPass) else ().pure[F]
        _ <- if (user.emailCode != EmailCode.ECA) AgingFile[F].writeUsernamePass(ldapUserName, newPass) else ().pure[F]
        _ <- GoogleAPI[F].changePassword(googleUserName, newPass)
        out <- SqlLiteDB[F].removeRecoveryLink(userName, extension).void
        _ <- Sync[F].delay(logger.info(s"Forgot Password Reset Completed for User: ${userName}"))
      } yield out
    }


  }

  private def concealEmail(email: String): String = {
    def obscure(text: String) = "*" * text.length
    val validEmail = "(.*)@(.*)".r
    val shortMailbox = "(.{1,2})".r
    val longMailbox = "(.)(.)(.*)".r

    email match {
      case validEmail(shortMailbox(m), domain) =>
        s"${obscure(m)}@$domain"
      case validEmail(longMailbox(first, second, middle), domain) =>
        s"$first$second${obscure(middle)}@$domain"
      case other => obscure(other)
    }
  }

  private def randomStringFromCharList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = scala.util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }

  private def randomAlphaNumeric(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('1' to '9')
    randomStringFromCharList(length, chars)
  }

}