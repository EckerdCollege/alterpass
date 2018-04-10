package edu.eckerd.alterpass.ldap

import edu.eckerd.alterpass.models.Configuration._
import edu.eckerd.alterpass.models.PasswordEncryptor
import fs2._
import cats.effect._
import cats.implicits._

trait Ldap[F[_]] {
  def checkBind(uid:String,pass:String) : F[Boolean]
  def setUserPassword(uid: String, newPass: String): F[Int]
  def changeUserPassword(uid: String, oldPass: String, newPass: String): F[Int]
}

object Ldap {

  def apply[F[_]](implicit ev: Ldap[F]): Ldap[F] = ev

  private val logger = org.log4s.getLogger

  def impl[F[_]: Sync](config: LdapConfig): Stream[F, Ldap[F]] = if (config.enabled) {
    val encryptor = if (config.activeDirectory) PasswordEncryptor.Base64 else PasswordEncryptor.Plain
    for {
      ldapAdmin <- Stream.bracket(Sync[F].delay(
        LdapAdmin(
          "ldaps",
          config.host,
          636,
          config.baseDN,
          config.searchAttribute,
          config.user,
          encryptor.encrypt(config.pass)
        )
      ))(_.pure[Stream[F, ?]], _.shutdown[F])
    } yield new Ldap[F]{
      def checkBind(uid:String,pass:String) : F[Boolean] = ldapAdmin.checkBind[F](uid, encryptor.encrypt(pass))
      def setUserPassword(uid: String, newPass: String): F[Int] = 
        ldapAdmin.setUserPassword[F](config.activeDirectory, uid, encryptor.encrypt(newPass))
      def changeUserPassword(uid: String, oldPass: String, newPass: String): F[Int] = 
        ldapAdmin.changeUserPassword[F](config.activeDirectory, uid, encryptor.encrypt(oldPass), encryptor.encrypt(newPass))
    }
  } else {
    val name = if (config.activeDirectory) "Active Directory" else "LDAP"
    new Ldap[F]{
      override def checkBind(uid:String,pass:String) : F[Boolean] = 
        Sync[F].delay(logger.info(s"$name Disabled: Acting As Though $uid was authenticated")).as(true)

      override def setUserPassword(uid: String, newPass: String): F[Int] =
        Sync[F].delay(logger.info(s"$name Disabled: Acting As Though $uid had password set successfully")).as(0)

      override def changeUserPassword(uid: String, oldPass: String, newPass: String): F[Int] =
        Sync[F].delay(logger.info(s"$name Disabled: Acting As Though $uid had password set successfully")).as(0)
    }.pure[Stream[F, ?]]
  }

  case object BindFailure extends Throwable

}
