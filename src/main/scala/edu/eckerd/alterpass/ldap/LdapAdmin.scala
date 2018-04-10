package edu.eckerd.alterpass.ldap

import com.unboundid.ldap.sdk._
import com.unboundid.util.ssl.{SSLUtil, TrustAllTrustManager}
import cats.effect._
import edu.eckerd.alterpass.models.{EncryptedString, PasswordEncryptor}
import cats.implicits._

private[ldap] case class LdapAdmin(
                ldapProtocol: String,
                ldapHost: String,
                ldapPort: Int,
                userBaseDN: String,
                searchAttribute: String,
                bindDN: String,
                bindPass: EncryptedString
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
      new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts,new SSLUtil(trustManager).createSSLSocketFactory()),new SimpleBindRequest(bindDN, bindPass.value), poolSize)
    case "ldap" =>
      new LDAPConnectionPool(new FailoverServerSet(serverAddresses, serverPorts),new SimpleBindRequest(bindDN, bindPass.value), poolSize)
    case _ =>
      null
  }

  private def search[F[_]: Sync](uid: String): F[List[SearchResultEntry]] = {
    import scala.collection.JavaConverters._
    val request = new SearchRequest(
      userBaseDN,
      SearchScope.SUB,
      Filter.createEqualityFilter(searchAttribute,uid)
    )
    Sync[F].delay(
      connectionPool.search(request).getSearchEntries.asScala.toList
    )
  }


  private def getFirstDN(entries: List[SearchResultEntry]) : Option[String] =
    entries.headOption.map(_.getDN)


  def bind[F[_]: Sync](uid: String, pass: EncryptedString): F[Int] = {
    val userDNT = search(uid).map(getFirstDN)
    userDNT.flatMap{
      case Some(dn) =>
        val bindRequest = new SimpleBindRequest(dn, pass.value)
        Sync[F].delay(
          connectionPool
            .bindAndRevertAuthentication(bindRequest)
            .getResultCode
            .intValue()
        )
          .attemptT
          .fold(_ => 1, identity)
      case None => Sync[F].pure(1)
    }
  }

  def checkBind[F[_]: Sync](uid:String,pass: EncryptedString) : F[Boolean] = {
    bind[F](uid, pass).map {
      case 0 => true
      case _ => false
    }
  }

  def getUserDN[F[_]: Sync](uid: String): F[Option[String]] = search[F](uid).map(getFirstDN)


  def setUserPassword[F[_]: Sync](activeDirectory: Boolean, uid: String, newPass: EncryptedString): F[Int] = {
    val userDNOpt: F[Option[String]] = getUserDN[F](uid)
    val modification = if (activeDirectory){
      new Modification(ModificationType.REPLACE, "unicodePwd", newPass.value)
    } else {
      new Modification(ModificationType.REPLACE, "userPassword", newPass.value)
    }
    val changePassword : Option[String] => F[Option[LDAPResult]] =
      userDN => Sync[F].delay(userDN.map(dn => connectionPool.modify(dn, modification)))
    val result = userDNOpt.flatMap(changePassword)
    result.map(_.map(_.getResultCode.intValue).getOrElse(1))
  }

  def changeUserPassword[F[_]: Sync](activeDirectory: Boolean, uid: String, oldPass: EncryptedString, newPass: EncryptedString): F[Int] = {
    checkBind[F](uid, oldPass).flatMap{
      case true => setUserPassword(activeDirectory, uid, newPass)
      case false => Sync[F].pure(1)
    }
  }

  def shutdown[F[_]: Sync] : F[Unit] = Sync[F].delay(connectionPool.close())

}
