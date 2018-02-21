package edu.eckerd.alterpass.http

import cats.implicits._
import org.log4s.getLogger
import cats.effect._
import edu.eckerd.alterpass.agingfile._
import edu.eckerd.alterpass.ldap._
import edu.eckerd.alterpass.google._

trait ChangePassword[F[_]]{
  def changePassword(username: String, oldPass: String, newPass: String): F[Unit]
}

object ChangePassword {
  def apply[F[_]](implicit ev: ChangePassword[F]) = ev
  private val logger = getLogger

  def impl[F[_]](implicit F: Effect[F], L: Ldap[F], A: AgingFile[F], G: GoogleAPI[F]): ChangePassword[F] = new ChangePassword[F]{
    def changePassword(username: String, oldPass: String, newPass: String): F[Unit] = {
      val ldapUserName = username.replaceAll("@eckerd.edu", "")
      val googleUserName = if (username.endsWith("@eckerd.edu")) username else s"${username}@eckerd.edu"

      Ldap[F].checkBind(username, oldPass).ifM(
        {
          for {
            _ <- AgingFile[F].writeUsernamePass(ldapUserName, newPass)
            _ <- Ldap[F].setUserPassword(ldapUserName, newPass)
            _ <- GoogleAPI[F].changePassword(googleUserName, newPass)
            out <- Sync[F].delay(logger.info(s"Change Password Reset Completed for User: $username"))
          } yield out
        },
        Sync[F].delay(logger.info(s"Failed Change Password Attempt - Invalid Old Password for $username")) *>
        Sync[F].raiseError[Unit](Ldap.BindFailure)
      )
    }
  }
}