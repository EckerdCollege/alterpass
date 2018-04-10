package edu.eckerd.alterpass.models

import org.specs2.mutable.Specification

class PasswordEncryptorSpec extends Specification {
  "PasswordEncryptor" should {
    "encode a known string correctly" in {
      val known = "1Tomato2!#"
      val output = PasswordEncryptor.Base64.encrypt(known)
      
      val expected = "IgAxAFQAbwBtAGEAdABvADIAIQAjACIA"
      expected must_===(output.value)
    }
  }
}