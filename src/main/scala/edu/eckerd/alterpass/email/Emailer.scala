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
    import scalatags.Text.all._

    div(
      p("""You are receiving this message because you have requested a reset of your reset of your password from
       |the forgot password site. If you have received this message in error please disregard.""".stripMargin
      ),
      p("""Your forgot password recovery link is""".stripMargin),
      p(
        a(s"$serverHostName/forgotpw/$random", href:=s"$serverHostName/forgotpw/$random")
      )
    ).render
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
