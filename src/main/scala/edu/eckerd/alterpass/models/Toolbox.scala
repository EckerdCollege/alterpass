package edu.eckerd.alterpass.models

import edu.eckerd.alterpass.agingfile.AgingFile
import edu.eckerd.alterpass.database.{OracleDB, SqlLiteDB}
import edu.eckerd.alterpass.email.Emailer
import edu.eckerd.alterpass.google.GoogleAPI
import edu.eckerd.alterpass.ldap.LdapAdmin
import org.http4s.server.blaze.BlazeBuilder

case class Toolbox(
                  agingFile: AgingFile,
                  ldapAdmin: LdapAdmin,
                  oracleDB: OracleDB,
                  sqlLiteDB: SqlLiteDB,
                  googleAPI: GoogleAPI,
                  blazeBuilder: BlazeBuilder,
                  email: Emailer
                  )
