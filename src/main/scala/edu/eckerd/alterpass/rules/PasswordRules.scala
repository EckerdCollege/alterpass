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
    override def validate(password: String): F[Unit] = (
      atleastEightCharsLong(password),
      numberInFirstEightChars(password),
      letterInFirstEightChars(password),
      doesNotContainInvalidCharacters(password)
    ).mapN((_, _, _, _) => ())
      .fold(
        nel => Sync[F].raiseError(ValidationFailure(nel)),
        _.pure[F]
      )
  }

  def atleastEightCharsLong(s: String): ValidatedNel[String, Unit] = 
    Validated.condNel[String, Unit](s.length >= 8, (), s"Password Too Short, Length:${s.length}")

  def numberInFirstEightChars(s: String): ValidatedNel[String, Unit] =
    Validated.condNel[String, Unit](s.take(8).matches(".*\\d+.*"), (), "No Number in First 8 Characters")

  def letterInFirstEightChars(s: String): ValidatedNel[String, Unit] = 
    Validated.condNel[String, Unit](s.take(8).matches(".*[a-zA-Z]+.*"), (), "No Alpha Character in First 8 Characters")

  def doesNotContainInvalidCharacters(s: String): ValidatedNel[String, Unit] = (
    Validated.condNel[String, Unit](!s.contains("\""), (), "Contains Invalid Character: '\"'"),
    Validated.condNel[String, Unit](!s.contains("'"), (), "Contains Invalid Character: \"'\""),
    Validated.condNel[String, Unit](!s.contains(":"), (), "Contains Invalid Character: ':'")
  ).mapN((_, _, _) => ())

  case class ValidationFailure(failures: NonEmptyList[String]) extends Throwable
}