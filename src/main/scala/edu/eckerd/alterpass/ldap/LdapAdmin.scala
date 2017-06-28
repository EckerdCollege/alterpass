package edu.eckerd.alterpass.ldap

import com.unboundid.ldap.sdk._
import com.unboundid.util.ssl.{SSLUtil, TrustAllTrustManager}
import fs2.{Strategy, Task}

class LdapAdmin(
                 ldapProtocol: String,
                 ldapHost: String,
                 ldapPort: Int,
                 userBaseDN: String,
                 searchAttribute: String,
                 bindDN: String,
                 bindPass: String
          )(implicit strategy: Strategy) {
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

  private def search(uid: String): Task[List[SearchResultEntry]] = {
    import scala.collection.JavaConverters._
    val request = new SearchRequest(
      userBaseDN,
      SearchScope.SUB,
      Filter.createEqualityFilter(searchAttribute,uid)
    )
    Task(
      connectionPool.search(request).getSearchEntries.asScala.toList
    )
  }


  private def getFirstDN(entries: List[SearchResultEntry]) : Option[String] =
    entries.headOption.map(_.getDN)


  def bind(uid: String, pass:String): Task[Int] = {
    val userDNT = search(uid).map(getFirstDN)
    userDNT.flatMap{
      case Some(dn) =>
        val bindRequest = new SimpleBindRequest(dn, pass)
        Task.delay(
          connectionPool
            .bindAndRevertAuthentication(bindRequest)
            .getResultCode
            .intValue()
        )
      case None => Task(1)
    }
  }

  def checkBind(uid:String,pass:String) : Task[Boolean] = {
    bind(uid, pass).map {
      case 0 => true
      case _ => false
    }
  }

  def getUserDN(uid: String): Task[Option[String]] = search(uid).map(getFirstDN)


  def setUserPassword(uid: String, newPass: String): Task[Int] = {
    val userDNOpt: Task[Option[String]] = getUserDN(uid)
    val modification = new Modification(ModificationType.REPLACE, "userPassword", newPass)
    val changePassword : Option[String] => Task[Option[LDAPResult]] =
      userDN => Task(userDN.map(dn => connectionPool.modify(dn, modification)))
    val result = userDNOpt.flatMap(changePassword)
    result.map(_.map(_.getResultCode.intValue).getOrElse(1))
  }

  def shutdown : Task[Unit] = Task.delay(connectionPool.close())

}

object LdapAdmin {


  def build(ldapProtocol: String,
            ldapHost: String,
            ldapPort: Int,
            userBaseDN: String,
            searchAttribute: String,
            bindDN: String,
            bindPass: String)(implicit strategy: Strategy): Task[LdapAdmin] = {

    Task(new LdapAdmin(ldapProtocol, ldapHost, ldapPort, userBaseDN, searchAttribute, bindDN, bindPass))
  }

}
