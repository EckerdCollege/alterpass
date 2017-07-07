package edu.eckerd.alterpass

import cats.data.Validated.{Invalid, Valid}
import edu.eckerd.alterpass.errors.ConfigErrors.SqlliteConfigError
import org.specs2.Specification

class ConfigurationSpec extends Specification {

  def is = s2"""
  getSqlLiteConfig should Parse a Failed SqlLite Config       $invalidGetSqlLiteConfig
  getSqlLiteConfig should Parse a Valid SqlLite Config        $validGetSqlLiteConfig
    """


  def invalidGetSqlLiteConfig = {
    val invalid = Invalid(SqlliteConfigError("SQLLITE_PATH missing from Environment")).toValidatedNel
    Configuration.getSqlLiteConfig(_ => None)  must_===  invalid
  }

  def validGetSqlLiteConfig = {
    val path = "/fake/path"
    val valid = Valid(Configuration.SqlLiteConfig(path)).toValidatedNel
    Configuration.getSqlLiteConfig(_ => Some(path)) must_=== valid
  }

}
