package edu.eckerd.alterpass.ldap

import fs2.Task

trait Ldap {

  def checkBind(uid:String,pass:String) : Task[Boolean]

  def setUserPassword(uid: String, newPass: String): Task[Int]

}
