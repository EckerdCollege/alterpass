organization := "edu.eckerd"
name := "alterpass"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.12.4"


val Http4sVersion = "0.18.0"
val CirceVersion = "0.9.1"
val DoobieVersion = "0.5.0"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

libraryDependencies ++= Seq(
  "org.http4s"      %% "http4s-blaze-server"  % Http4sVersion,
  "org.http4s"      %% "http4s-circe"         % Http4sVersion,
  "org.http4s"      %% "http4s-dsl"           % Http4sVersion,
  "io.circe"        %% "circe-core"           % CirceVersion,
  "io.circe"        %% "circe-generic"        % CirceVersion,
  "io.circe"        %% "circe-parser"         % CirceVersion,
  "org.tpolecat"    %% "doobie-core"          % DoobieVersion,
  "org.tpolecat"    %% "doobie-hikari"        % DoobieVersion,
  "com.beachape"    %% "enumeratum"           % "1.5.12",
  "com.github.pureconfig" %% "pureconfig"     % "0.9.0",
  "com.lihaoyi"     %% "scalatags"            % "0.6.7",
  "org.xerial"      % "sqlite-jdbc"           % "3.19.3",
  "edu.eckerd"      %% "google-api-scala"     % "0.1.2",
  "ch.qos.logback"  %  "logback-classic"      % "1.2.1",
  "com.unboundid"   %  "unboundid-ldapsdk"    % "4.0.0",
  "javax.mail"      % "javax.mail-api"        % "1.5.6",
  "com.sun.mail"    % "javax.mail"            % "1.5.2",
  "org.specs2"      %% "specs2-core"          % "3.9.1"         % Test,
  "org.http4s"      %% "http4s-testing"       % Http4sVersion   % Test,
  "org.tpolecat"    %% "doobie-specs2"        % DoobieVersion   % Test

)
