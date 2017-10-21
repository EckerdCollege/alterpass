package edu.eckerd.alterpass.http

import cats.data.{NonEmptyList, OptionT}
import cats.effect.IO
import org.http4s.CacheDirective.`no-cache`
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.`Cache-Control`


object StaticSite {

  val supportedStaticExtensions =
    
  List(".html", ".js", ".map", ".css", ".png", ".ico", ".jpg", ".jpeg", ".otf", ".ttf"  )

  val service = HttpService[IO] {
    // Does An HTML Rewrite of html files so that it does not display the .html
    case req @ GET -> Root =>
      StaticFile
        .fromResource(s"/pages/index.html", Some(req))
        .map(_.putHeaders())
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .getOrElseF(NotFound())

     // Loads Any Static Resources as Called
    case req if supportedStaticExtensions.exists(req.pathInfo.endsWith) =>
      StaticFile
        .fromResource(req.pathInfo, Some(req))
        .map(_.putHeaders())
        .orElse(
          OptionT.fromOption[IO](Option(getClass.getResource(req.pathInfo))
          ).flatMap(StaticFile.fromURL(_, Some(req)))
        )
        .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
        .getOrElseF(NotFound())
  }
}
