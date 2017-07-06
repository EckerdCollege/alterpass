package edu.eckerd.alterpass

import cats.Apply
import fs2.Task
import cats.data.{ValidatedNel, _}
import cats.implicits._
import edu.eckerd.alterpass.errors.ConfigErrors
import errors.ConfigErrors._

import scala.util.Properties.envOrNone
import scala.util.Try

object Configuration {

  case class ApplicationConfig(
                              httpConfig: HttpConfig,
                              oracleConfig: OracleConfig,
                              googleConfig: GoogleConfig,
                              sqlLiteConfig: SqlLiteConfig,
                              agingFileConfig: AgingFileConfig,
                              ldapConfig: LdapConfig
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

  def loadAllFromEnv(): Task[ApplicationConfig] = {
    getAppConfig(envOrNone).fold(nelTask, configTask)
  }

  val nelTask : NonEmptyList[ConfigErrors] => Task[ApplicationConfig] = nel => {
    val message = nel.map(_.message).toList.mkString("\n")
    val thrower = new Throwable(message)
    val t : Task[ApplicationConfig] = Task.fail(thrower)
    t
  }
  val configTask: ApplicationConfig => Task[ApplicationConfig] = a => Task.now(a)



  def getAppConfig(f: String => Option[String]): ValidatedNel[ConfigErrors, ApplicationConfig] =
    Apply[ValidatedNel[ConfigErrors, ?]].map6(
      getHttpConfig(f),
      getOracleConfig(f),
      getGoogleConfig(f),
      getSqlLiteConfig(f),
      getAgingFileConfig(f),
      getLdapConfig(f)
    ) {
      case (http, oracle, google, sqllite, aging, ldap) => ApplicationConfig(http, oracle, google, sqllite, aging, ldap)
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
