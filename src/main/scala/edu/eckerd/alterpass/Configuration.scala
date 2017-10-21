package edu.eckerd.alterpass

import cats.Apply
import cats.data.{ValidatedNel, _}
import cats.implicits._
import edu.eckerd.alterpass.agingfile.AgingFile
import edu.eckerd.alterpass.database.{OracleDB, SqlLiteDB}
import edu.eckerd.alterpass.errors.ConfigErrors
import edu.eckerd.alterpass.google.GoogleAPI
import edu.eckerd.alterpass.ldap.LdapAdmin
import edu.eckerd.alterpass.models.Toolbox
import errors.ConfigErrors._
import org.http4s.server.blaze.BlazeBuilder
import cats.effect.IO

import scala.util.Properties.envOrNone
import scala.util.Try

object Configuration {

  case class ApplicationConfig(
                              httpConfig: HttpConfig,
                              oracleConfig: OracleConfig,
                              googleConfig: GoogleConfig,
                              sqlLiteConfig: SqlLiteConfig,
                              agingFileConfig: AgingFileConfig,
                              ldapConfig: LdapConfig,
                              emailConfig: EmailConfig
                              )

  case class HttpConfig(
                       port: Int,
                       hostname: String
                       )

  case class OracleConfig(
                           host: String,
                           port: Int,
                           sid: String,
                           username: String,
                           pass: String
                         )

  case class GoogleConfig(
                           domain: String,
                           serviceAccount: String,
                           administratorAccount: String,
                           credentialFilePath: String,
                           applicationName: String
                         )

  case class LdapConfig(
                       host: String,
                       baseDN: String,
                       searchAttribute: String,
                       user: String,
                       pass: String
                       )

  case class SqlLiteConfig(absolutePath: String)
  case class AgingFileConfig(absolutePath: String)

  def loadAllFromEnv(): IO[ApplicationConfig] = {
    getAppConfig(envOrNone).fold(nelTask, configTask)
  }

  val nelTask : NonEmptyList[ConfigErrors] => IO[ApplicationConfig] = nel => {
    val message = nel.map(_.message).toList.mkString("\n")
    val thrower = new Throwable(message)
    val t : IO[ApplicationConfig] = IO.raiseError(thrower)
    t
  }
  val configTask: ApplicationConfig => IO[ApplicationConfig] = a => IO.pure(a)



  def getAppConfig(f: String => Option[String]): ValidatedNel[ConfigErrors, ApplicationConfig] =
    Apply[ValidatedNel[ConfigErrors, ?]].map7(
      getHttpConfig(f),
      getOracleConfig(f),
      getGoogleConfig(f),
      getSqlLiteConfig(f),
      getAgingFileConfig(f),
      getLdapConfig(f),
      getEmailConfig(f)
    ) {
      case (http, oracle, google, sqllite, aging, ldap, email) =>
        ApplicationConfig(http, oracle, google, sqllite, aging, ldap, email)
    }

  def getLdapConfig(f: String => Option[String]): ValidatedNel[LdapConfigError, LdapConfig] = {
    val hostEnv = "LDAP_HOST"
    val dnEnv = "LDAP_BASE_DN"
    val searchEnv = "LDAP_SEARCH_ATTR"
    val userEnv = "LDAP_USER"
    val passEnv = "LDAP_PASS"

    val validateL: String => ValidatedNel[LdapConfigError, String] =
      s => Validated.fromOption(f(s), LdapConfigError(s"$s missing from Environment")).toValidatedNel

    Apply[ValidatedNel[LdapConfigError, ?]].map5(
      validateL(hostEnv), validateL(dnEnv), validateL(searchEnv), validateL(userEnv), validateL(passEnv)
    ) {
      case (host, baseDN, searchAttr, user, pass) => LdapConfig(host, baseDN, searchAttr, user, pass)
    }
  }

  def getAgingFileConfig(f: String => Option[String]): ValidatedNel[AgingFileConfigError, AgingFileConfig] = {
    val pathEnv = "AGING_FILE_PATH"
    Apply[ValidatedNel[AgingFileConfigError, ?]].map(
      Validated.fromOption(f(pathEnv), AgingFileConfigError(s"$pathEnv missing from Environment")).toValidatedNel
    ) {
      case (path) => AgingFileConfig(path)
    }
  }

  def getSqlLiteConfig(f: String => Option[String]): ValidatedNel[SqlliteConfigError, SqlLiteConfig] = {
    val pathEnv = "SQLLITE_PATH"

    Apply[ValidatedNel[SqlliteConfigError, ?]].map(
      Validated.fromOption(f(pathEnv), SqlliteConfigError(s"$pathEnv missing from Environment")).toValidatedNel
    ) {
      case (path) => SqlLiteConfig(path)
    }
  }

  case class EmailConfig(
                          host: String,
                          user: String,
                          pass: String,
                          baseLink: String
                        )

  def getEmailConfig(f: String => Option[String]): ValidatedNel[EmailConfigError, EmailConfig] = {
    val hostEnv = "SMTP_HOSTNAME"
    val userEnv = "SMTP_USER"
    val passEnv = "SMTP_PASS"
    val linkEnv = "EMAIL_LINK"


    Apply[ValidatedNel[EmailConfigError, ?]].map4(
      Validated.fromOption(f(hostEnv), EmailConfigError(s"$hostEnv missing from Environment")).toValidatedNel,
      Validated.fromOption(f(userEnv), EmailConfigError(s"$userEnv missing from Environment")).toValidatedNel,
      Validated.fromOption(f(passEnv), EmailConfigError(s"$passEnv missing from Environment")).toValidatedNel,
      Validated.fromOption(f(linkEnv), EmailConfigError(s"$linkEnv missing from Environment")).toValidatedNel
    ) {
      case (hostname, user, pass, link) => EmailConfig(hostname, user, pass, link)
    }
  }

  def getHttpConfig(f: String => Option[String]): ValidatedNel[HttpConfigError, HttpConfig] = {
    val portEnv = "HTTP_PORT"
    val hostEnv = "HTTP_HOSTNAME"

    val port = f(portEnv).flatMap(s => Try(s.toInt).toOption).getOrElse(8080)
    val host = f(hostEnv).getOrElse("0.0.0.0")

    Validated.Valid(HttpConfig(port, host))
  }

  def getGoogleConfig(f: String => Option[String]): ValidatedNel[GoogleConfigError, GoogleConfig] = {
    val domainEnv = "GOOGLE_DOMAIN"
    val serviceEnv = "GOOGLE_SERVICE_ACCOUNT"
    val adminEnv = "GOOGLE_ADMIN_ACCOUNT"
    val credEnv = "GOOGLE_CREDENTIALS_FILE"
    val appEnv = "GOOGLE_APP_NAME"

    val validateG : String => ValidatedNel[GoogleConfigError, String] = str =>
      Validated.fromOption(f(str), GoogleConfigError(s"$str missing from Environment")).toValidatedNel

    Apply[ValidatedNel[GoogleConfigError, ?]].map5(
      validateG(domainEnv),
      validateG(serviceEnv),
      validateG(adminEnv),
      validateG(credEnv),
      validateG(appEnv)
    ) {
      case (domain, service, admin, credential, appName) =>
        GoogleConfig(domain, service, admin, credential, appName)
    }
  }


  def getOracleConfig(f: String => Option[String]): ValidatedNel[OracleConfigError, OracleConfig] = {
    val hostEnv = "ORACLE_HOST"
    val portEnv = "ORACLE_PORT"
    val sidEnv = "ORACLE_SID"
    val userEnv = "ORACLE_USER"
    val passEnv = "ORACLE_PASS"

    def validateO(s: String): Validated[OracleConfigError, String] = {
      Validated.fromOption(f(s), OracleConfigError(s"$s missing from Environment"))
    }

    val host = validateO(hostEnv)
    val port = Validated.fromOption(f(portEnv).flatMap(p => Try(p.toInt).toOption), OracleConfigError(s"$portEnv either missing or invalid"))
    val sid = validateO(sidEnv)
    val user = validateO(userEnv)
    val pass = validateO(passEnv)

    val OracleConfigFromEnv : ValidatedNel[OracleConfigError, OracleConfig] =
      Apply[ValidatedNel[OracleConfigError, ?]].map5(
        host.toValidatedNel, port.toValidatedNel, sid.toValidatedNel, user.toValidatedNel, pass.toValidatedNel
      ) {
        case (host, port, sid, user, pass) => OracleConfig(host, port, sid, user, pass)
      }

    OracleConfigFromEnv
  }






}
