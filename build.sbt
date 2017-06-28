organization := "edu.eckerd"
name := "alterpass"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.2"

val Http4sVersion = "0.17.0-M3"

libraryDependencies ++= Seq(
  "org.http4s"      %% "http4s-blaze-server"  % Http4sVersion,
  "org.http4s"      %% "http4s-circe"         % Http4sVersion,
  "org.http4s"      %% "http4s-dsl"           % Http4sVersion,
  "edu.eckerd"      %% "google-api-scala"     % "0.1.1",
  "ch.qos.logback"  %  "logback-classic"      % "1.2.1",
  "com.unboundid"   %  "unboundid-ldapsdk"    % "4.0.0"
)
