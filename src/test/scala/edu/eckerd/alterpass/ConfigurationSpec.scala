package edu.eckerd.alterpass

import cats.data.Validated.Invalid
import edu.eckerd.alterpass.errors.ConfigErrors.SqlliteConfigError
import org.specs2.Specification

class ConfigurationSpec extends Specification {

  def is = s2"""
  getSqlLiteConfig should Parse a Failed SqlLite Config      $invalidGetSqlLiteConfig
    """


  def invalidGetSqlLiteConfig = {
    val invalid = Invalid(SqlliteConfigError("SQLLITE_PATH missing from Environment")).toValidatedNel
    Configuration.getSqlLiteConfig(_ => None)  must_===  invalid
  }

}
