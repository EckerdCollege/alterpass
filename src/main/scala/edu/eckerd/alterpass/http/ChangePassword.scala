package edu.eckerd.alterpass.http

import cats.implicits._
import org.log4s.getLogger
import cats.effect._
import edu.eckerd.alterpass.agingfile._
import edu.eckerd.alterpass.ldap._
import edu.eckerd.alterpass.google._
import edu.eckerd.alterpass.rules._

trait ChangePassword[F[_]]{
  def changePassword(username: String, oldPass: String, newPass: String): F[Unit]
}

object ChangePassword {
  def apply[F[_]](implicit ev: ChangePassword[F]) = ev
  private val logger = getLogger

  def impl[F[_]](implicit F: Effect[F], L: Ldap[F], A: AgingFile[F], G: GoogleAPI[F], P: PasswordRules[F]): ChangePassword[F] = new ChangePassword[F]{
    def changePassword(username: String, oldPass: String, newPass: String): F[Unit] = {
      val lowerCaseUsername = username.toLowerCase
      val ldapUserName = lowerCaseUsername.replaceAll("@eckerd.edu", "")
      val googleUserName = if (lowerCaseUsername.endsWith("@eckerd.edu")) lowerCaseUsername else s"${lowerCaseUsername}@eckerd.edu"

      P.validate(newPass) *>
      Ldap[F].checkBind(ldapUserName, oldPass).ifM(
        {
          for {
            _ <- AgingFile[F].writeUsernamePass(ldapUserName, newPass)
            _ <- Ldap[F].setUserPassword(ldapUserName, newPass)
            _ <- GoogleAPI[F].changePassword(googleUserName, newPass)
            out <- Sync[F].delay(logger.info(s"Change Password Reset Completed for User: $ldapUserName"))
          } yield out
        },
        Sync[F].delay(logger.info(s"Failed Change Password Attempt - Invalid Old Password for $ldapUserName")) *>
        Sync[F].raiseError[Unit](Ldap.BindFailure)
      )
    }
  }
}