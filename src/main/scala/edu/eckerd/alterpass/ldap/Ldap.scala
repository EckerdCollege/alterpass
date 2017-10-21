package edu.eckerd.alterpass.ldap

import cats.effect.IO

trait Ldap {

  def checkBind(uid:String,pass:String) : IO[Boolean]

  def setUserPassword(uid: String, newPass: String): IO[Int]

}
