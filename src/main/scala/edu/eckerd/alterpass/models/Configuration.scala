package edu.eckerd.alterpass.models

object Configuration {

  case class ApplicationConfig(
                              httpConfig: HttpConfig,
                              oracleConfig: OracleConfig,
                              googleConfig: GoogleConfig,
                              sqlLiteConfig: SqlLiteConfig,
                              agingFileConfig: AgingFileConfig,
                              ldapConfig: LdapConfig,
                              activeDirectoryConfig: LdapConfig,
                              emailConfig: EmailConfig
                              )

  case class HttpConfig(
                       port: Int,
                       hostname: String
                       )

  case class OracleConfig(
                           enabled: Boolean,
                           host: String,
                           port: Int,
                           sid: String,
                           username: String,
                           pass: String
                         )

  case class GoogleConfig(
                           enabled: Boolean,
                           domain: String,
                           serviceAccount: String,
                           administratorAccount: String,
                           credentialFilePath: String,
                           applicationName: String
                         )

  case class LdapConfig(
                       enabled: Boolean,
                       host: String,
                       activeDirectory: Boolean,
                       baseDN: String,
                       searchAttribute: String,
                       user: String,
                       pass: String
                       )

  case class SqlLiteConfig(absolutePath: String)
  case class AgingFileConfig(absolutePath: String)

  case class EmailConfig(
                          enabled: Boolean,
                          host: String,
                          user: String,
                          pass: String,
                          baseLink: String
                        )

}
