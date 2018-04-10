package edu.eckerd.alterpass.models

import java.util.{Base64 => B64}

sealed trait PasswordEncryptor {
  def encrypt: String => EncryptedString
}
object PasswordEncryptor {
  case object Plain extends PasswordEncryptor {
    override def encrypt : String => EncryptedString = 
      s => EncryptedString(s)
  }
  case object Base64 extends PasswordEncryptor {
    override def encrypt: String => EncryptedString = string => {
      val startPass : String = "\"" + string + "\""
      val utf16Pass : Array[Byte] =  startPass.getBytes("UTF-16LE")
      // val encodedBytes = B64.getEncoder().encode(utf16Pass.getBytes())
      val encodedString = new String(utf16Pass)
      EncryptedString(encodedString)
    }
  }
}

case class EncryptedString(value: String)