package edu.eckerd.alterpass.google

import edu.eckerd.google.api.services.directory.Directory
import fs2._
import edu.eckerd.google.api.services.Scopes.ADMIN_DIRECTORY
import edu.eckerd.google.api.services.directory.models.User
import cats.effect.IO

class GoogleAPI(
                 domain: String,
                 serviceAccount: String,
                 administratorAccount: String,
                 credentialFilePath: String,
                 applicationName: String) {

  private val adminDirectory = Directory(
    serviceAccount,
    administratorAccount,
    credentialFilePath,
    applicationName,
    ADMIN_DIRECTORY
  )

  def changePassword(user: String, password: String): IO[User] = {
    IO(adminDirectory.users.get(user))
      .flatMap(_.fold(IO.raiseError, IO.pure))
      .map(currentUser => currentUser.copy(password = Some(password)))
      .flatMap( preUpdatedUser => IO(adminDirectory.users.update(preUpdatedUser)))
  }

}

object GoogleAPI {

  def build(
             domain: String,
             serviceAccount: String,
             administratorAccount: String,
             credentialFilePath: String,
             applicationName: String): IO[GoogleAPI] = {
    IO(new GoogleAPI(domain, serviceAccount, administratorAccount, credentialFilePath, applicationName))
  }


}
