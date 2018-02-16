package edu.eckerd.alterpass.google

import edu.eckerd.alterpass.models.Configuration._
import edu.eckerd.google.api.services.directory.Directory
import edu.eckerd.google.api.services.Scopes.ADMIN_DIRECTORY
import cats.implicits._
import cats.effect._
import fs2._

trait GoogleAPI[F[_]]{
  def changePassword(user: String, password: String): F[Unit]
}

object GoogleAPI {
  def apply[F[_]](implicit ev: GoogleAPI[F]): GoogleAPI[F] = ev

  private val logger = org.log4s.getLogger


  def impl[F[_]: Sync](config: GoogleConfig): Stream[F, GoogleAPI[F]] = if (config.enabled){
    for {
      adminDirectory <- Stream.eval(Sync[F].delay(
        Directory(
          config.serviceAccount,
          config.administratorAccount,
          config.credentialFilePath,
          config.applicationName,
          ADMIN_DIRECTORY
        )
      ))
    } yield new GoogleAPI[F]{
      override def changePassword(user: String, password: String): F[Unit] = for {
        _ <- Sync[F].delay(logger.trace(s"Attempting to get User Info From Google For $user"))
        user <- Sync[F].delay(adminDirectory.users.get(user)).rethrow
        _ <- Sync[F].delay(logger.trace(s"User Info Returned From Google, Updating User Password in Google for $user"))
        updatedUser = user.copy(password = Some(password))
        success <- Sync[F].delay(adminDirectory.users.update(updatedUser)).void
        _ <- Sync[F].delay(logger.trace(s"User Password Successfully Updated in Google for $user"))
      } yield success
    }

  } else {
    Stream(
      new GoogleAPI[F]{
        override def changePassword(user: String, password: String): F[Unit] = 
          Sync[F].delay(logger.info(
            s"GoogleAPI is disabled. Password would have been changed for $user if it was enabled"
          ))
      }
    )

  }
}