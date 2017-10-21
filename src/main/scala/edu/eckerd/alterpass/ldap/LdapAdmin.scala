package edu.eckerd.alterpass.ldap

import com.unboundid.ldap.sdk._
import com.unboundid.util.ssl.{SSLUtil, TrustAllTrustManager}
import cats.effect.IO
import cats._
import cats.implicits._

class LdapAdmin(
                 ldapProtocol: String,
                 ldapHost: String,
                 ldapPort: Int,
                 userBaseDN: String,
                 searchAttribute: String,
                 bindDN: String,
                 bindPass: String
          ) {
  val poolSize = 5

  val serverAddresses = Array(ldapHost)
  val serverPorts = Array(ldapPort)

  val trustManager = {
    ldapProtocol match {
      case "ldaps" =>
        new TrustAllTrustManager()
      case _ =>
        null// don't need a trust store
    }
  }

  // Initialize Multi-Server LDAP Connection Pool
  val connectionPool : LDAPConnectionPool = ldapProtocol match {
    case "ldaps" =>
      new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts,new SSLUtil(trustManager).createSSLSocketFactory()),new SimpleBindRequest(bindDN, bindPass), poolSize)
    case "ldap" =>
      new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts),new SimpleBindRequest(bindDN, bindPass), poolSize)
    case _ =>
      null
  }

  private def search(uid: String): IO[List[SearchResultEntry]] = {
    import scala.collection.JavaConverters._
    val request = new SearchRequest(
      userBaseDN,
      SearchScope.SUB,
      Filter.createEqualityFilter(searchAttribute,uid)
    )
    IO(
      connectionPool.search(request).getSearchEntries.asScala.toList
    )
  }


  private def getFirstDN(entries: List[SearchResultEntry]) : Option[String] =
    entries.headOption.map(_.getDN)


  def bind(uid: String, pass:String): IO[Int] = {
    val userDNT = search(uid).map(getFirstDN)
    userDNT.flatMap{
      case Some(dn) =>
        val bindRequest = new SimpleBindRequest(dn, pass)
        IO(
          connectionPool
            .bindAndRevertAuthentication(bindRequest)
            .getResultCode
            .intValue()
        )
          .attemptT
          .fold(_ => 1, identity)
      case None => IO(1)
    }
  }

  def checkBind(uid:String,pass:String) : IO[Boolean] = {
    bind(uid, pass).map {
      case 0 => true
      case _ => false
    }
  }

  def getUserDN(uid: String): IO[Option[String]] = search(uid).map(getFirstDN)


  def setUserPassword(uid: String, newPass: String): IO[Int] = {
    val userDNOpt: IO[Option[String]] = getUserDN(uid)
    val modification = new Modification(ModificationType.REPLACE, "userPassword", newPass)
    val changePassword : Option[String] => IO[Option[LDAPResult]] =
      userDN => IO(userDN.map(dn => connectionPool.modify(dn, modification)))
    val result = userDNOpt.flatMap(changePassword)
    result.map(_.map(_.getResultCode.intValue).getOrElse(1))
  }

  def changeUserPassword(uid: String, oldPass: String, newPass: String): IO[Int] = {
    checkBind(uid, oldPass).flatMap{
      case true => setUserPassword(uid, newPass)
      case false => IO.pure(1)
    }
  }

  def shutdown : IO[Unit] = IO(connectionPool.close())

}

object LdapAdmin {


  def build(ldapProtocol: String,
            ldapHost: String,
            ldapPort: Int,
            userBaseDN: String,
            searchAttribute: String,
            bindDN: String,
            bindPass: String): IO[LdapAdmin] = {
    IO(new LdapAdmin(ldapProtocol, ldapHost, ldapPort, userBaseDN, searchAttribute, bindDN, bindPass))
  }

}
