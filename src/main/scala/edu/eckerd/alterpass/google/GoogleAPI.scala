package edu.eckerd.alterpass.google

import edu.eckerd.google.api.services.directory.Directory
import fs2._
import edu.eckerd.google.api.services.Scopes.ADMIN_DIRECTORY
import edu.eckerd.google.api.services.directory.models.User

class GoogleAPI(
                 domain: String,
                 serviceAccount: String,
                 administratorAccount: String,
                 credentialFilePath: String,
                 applicationName: String)(implicit strategy: Strategy) {

  private val adminDirectory = Directory(
    serviceAccount,
    administratorAccount,
    credentialFilePath,
    applicationName,
    ADMIN_DIRECTORY
  )


  def changePassword(user: String, password: String): Task[User] = {
    Task(adminDirectory.users.get(user))
      .flatMap(_.fold(Task.fail, Task.apply(_)))
      .map(currentUser => currentUser.copy(password = Some(password)))
      .flatMap( preUpdatedUser => Task(adminDirectory.users.update(preUpdatedUser)))
  }


}

object GoogleAPI {

  def build(
             domain: String,
             serviceAccount: String,
             administratorAccount: String,
             credentialFilePath: String,
             applicationName: String)(implicit strategy: Strategy): Task[GoogleAPI] = {
    Task(new GoogleAPI(domain, serviceAccount, administratorAccount, credentialFilePath, applicationName)(strategy))


  }


}
