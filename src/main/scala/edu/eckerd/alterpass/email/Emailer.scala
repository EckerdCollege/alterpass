package edu.eckerd.alterpass.email

import javax.mail._
import java.util.{Date, Properties}
import javax.mail.internet.{InternetAddress, MimeMessage}
import edu.eckerd.alterpass.models.Configuration.EmailConfig
import cats.effect.Sync

case class Emailer(config: EmailConfig) {

  private val smtpServer = config.host
  private val user = config.user
  private val pass = config.pass
  private val baseLink = config.baseLink

  val userEmail: String = if (user.endsWith("@eckerd.edu")) user else s"$user@eckerd.edu"

  private val properties: Properties = {
    val props = new Properties() // As in We don't reassing, but we do change internal structure with following calls.
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
    val messageLink = s"$baseLink/forgotpw/$random"
    s"""
      |<style>
      |@font-face {
      |    font-family: din;
      |    src: url("$baseLink/static/fonts/DIN-Regular.ttf");
      |}
      |@font-face {
      |    font-family: dincond-bold;
      |    src: url("$baseLink/static/fonts/dincond-bold.otf");
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
      |<div align="left">
      |<img src="$baseLink/static/img/EC-GulfCoast.png" align="center">
      |</div>
      |<h2 style="font-family:dincond-bold; color: #38939b; border-bottom: 2px solid #38939b;"> Eckerd College Password Recovery </h2>
      |
      |<p style="font-family:din;"> You are receiving this message because you have requested a reset of your Eckerd College password from
      |our forgot password site. If you have received this message in error, please disregard. <br> <br> Please note that this link will expire in 24 hours.</p>
      |
      |<p style="font-family:din;"> Your password recovery link is: </p>
      |<p style="font-family:din;"><a href="$messageLink">$messageLink</a></p>
      |</div>
      |
      |
      |</div>
    """.stripMargin
  }


  def sendNotificationEmail[F[_]: Sync](emails: List[String], random: String): F[Unit] = Sync[F].delay{
    if (emails.nonEmpty) {
      val message = new MimeMessage(session)
      val from = new InternetAddress(userEmail)
      from.setPersonal("Eckerd College ITS")
      message.setFrom(from)
      emails.foreach(email =>
        message.setRecipients(Message.RecipientType.TO, email)
      )
      message.setRecipients(Message.RecipientType.TO, emails.mkString(", "))
      message.setSubject("Eckerd College Password Reset")
      message.setContent(htmlMessage(random), "text/html; charset=utf-8")
      message.setSentDate(new Date())

      Transport.send(message)
    } else {
      ()
    }
  }

}
