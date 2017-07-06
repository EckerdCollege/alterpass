organization := "edu.eckerd"
name := "alterpass"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.12.2"


val Http4sVersion = "0.17.0-M3"
val CirceVersion = "0.8.0"
val DoobieVersion = "0.4.2-SNAPSHOT"

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "org.http4s"      %% "http4s-blaze-server"  % Http4sVersion,
  "org.http4s"      %% "http4s-circe"         % Http4sVersion,
  "org.http4s"      %% "http4s-dsl"           % Http4sVersion,
  "io.circe"        %% "circe-core"           % CirceVersion,
  "io.circe"        %% "circe-generic"        % CirceVersion,
  "io.circe"        %% "circe-parser"         % CirceVersion,
  "org.tpolecat"    %% "doobie-core-cats"     % DoobieVersion,
  "edu.eckerd"      %% "google-api-scala"     % "0.1.1",
  "ch.qos.logback"  %  "logback-classic"      % "1.2.1",
  "com.unboundid"   %  "unboundid-ldapsdk"    % "4.0.0",
  "org.specs2"      %% "specs2-core"          % "3.9.1"         % Test,
  "org.http4s"      %% "http4s-testing"       % Http4sVersion   % Test,
  "org.tpolecat"    %% "doobie-specs2-cats"   % DoobieVersion   % Test
)
