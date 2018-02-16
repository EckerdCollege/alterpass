package edu.eckerd.alterpass.email

import cats.effect._
import fs2._
import edu.eckerd.alterpass.models.Configuration._

trait EmailService[F[_]]{
  def sendNotificationEmail(emails: List[String], random: String): F[Unit]
}

object EmailService {
  def apply[F[_]](implicit ev: EmailService[F]): EmailService[F] = ev

  private val logger = org.log4s.getLogger

  def impl[F[_]: Sync](config: EmailConfig): Stream[F, EmailService[F]] = 
    if (config.enabled){
        Stream.eval(Sync[F].delay(Emailer(config))).map{ emailer => 
          new EmailService[F]{
              override def sendNotificationEmail(emails: List[String], random: String): F[Unit] = {
                  emailer.sendNotificationEmail[F](emails, random)
              }
          }
      }
    } else {
      Stream(
        new EmailService[F]{
            override def sendNotificationEmail(emails: List[String], random: String): F[Unit] = {
                Sync[F].delay(logger.info(s"Email Service Disabled: Email Would Have Been Sent to $emails - Extension: $random"))
            }

        }
      )

    }
}