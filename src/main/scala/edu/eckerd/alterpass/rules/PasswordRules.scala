package edu.eckerd.alterpass.rules

import cats.data._
import cats.effect._
import cats.implicits._


trait PasswordRules[F[_]]{
  def validate(password: String): F[Unit]
}

object PasswordRules {
  def apply[F[_]](implicit ev: PasswordRules[F]): PasswordRules[F] = ev
  def impl[F[_]: Sync]: PasswordRules[F] = new PasswordRules[F]{
    override def validate(password: String): F[Unit] = ().pure[F]
  }

  case class PasswordFailure(failures: NonEmptyList[String])
}