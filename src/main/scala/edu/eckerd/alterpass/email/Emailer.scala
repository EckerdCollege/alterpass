package edu.eckerd.alterpass.email

import javax.mail.{internet, _}
import java.util.{Date, Properties}
import javax.mail.internet.{InternetAddress, MimeMessage, MimeMultipart}

import cats.data.NonEmptyList
import fs2.Task
import fs2.Strategy



case class Emailer(smtpServer: String, serverHostName: String, user: String, pass: String) {

  val userEmail: String = if (user.endsWith("@eckerd.edu")) user else s"$user@eckerd.edu"

  private val properties: Properties = {
    var props = new Properties()
    props.put("mail.smtp.auth", "true")
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", smtpServer)
    props.put("mail.smtp.port", "587")

    props
  }

  private val authenticator = new javax.mail.Authenticator {
    override def getPasswordAuthentication(): PasswordAuthentication =
      new PasswordAuthentication(userEmail, pass)
  }

  private val session: Session = Session.getInstance(properties, authenticator)

  def htmlMessage(random: String): String = {
    val link = s"http://$serverHostName/forgotpw/$random"
    s"""
      |<style>
      |@font-face {
      |    font-family: din;
      |    src: url("http://$serverHostName/static/fonts/DIN-Regular.ttf");
      |}
      |@font-face {
      |    font-family: dincond-bold;
      |    src: url("http://$serverHostName/static/fonts/dincond-bold.otf");
      |}
      |</style>
      |
      |
      |
      |<div style="min-height:100%;">
      |
      |<div style="background: linear-gradient(#00a3c9, #bdcc2a); width:150px; height:100vh;float:right; "></div>
      |<div style="background: linear-gradient(#00a3c9, #bdcc2a); width:150px; height:100vh;float:left;"></div>
      |<div style="margin: 30px auto 30px; padding: 0 0 40px;">
      |<div align="center">
      |<img src="http://$serverHostName/static/img/eckerd_logo_email.jpg" align="center">
      |</div>
      |<h2 style="font-family:dincond-bold; color: #38939b; border-bottom: 2px solid #38939b;"> Eckerd College Password Recovery </h1>
      |
      |<p style="font-family:din;"> You are receiving this message because you have requested a reset of your Eckerd College password from
      |our forgot password site. If you have received this message in error, please disregard. </p>
      |
      |<p style="font-family:din;"> Your password recovery link is: </p>
      |<p style="font-family:din;"><a href="$link">$link</a></p>
      |</div>
      |
      |
      |</div>
    """.stripMargin
  }


  def sendNotificationEmail(emails: List[String], random: String)(implicit strategy: Strategy): Task[Unit] = Task.now{
    if (emails.nonEmpty) {
      val message = new MimeMessage(session)
      val from = new InternetAddress(userEmail)
      from.setPersonal("Eckerd College ITS")
      message.setFrom(from)
      emails.foreach(email =>
        message.setRecipients(Message.RecipientType.TO, email)
      )
      message.setRecipients(Message.RecipientType.TO, emails.toList.mkString(", "))
      message.setSubject("Eckerd College Password Reset")
      message.setContent(htmlMessage(random), "text/html; charset=utf-8")
      //    message.setText(htmlMessage(random), "utf-8", "html")
      message.setSentDate(new Date())

      Transport.send(message)
    } else {
      ()
    }
  }

}
