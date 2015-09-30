name := "akka-streams-http-intro"

organization := "com.lancearlaus"

version := "0.1"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental"           % "1.0",
  "com.typesafe.akka" %% "akka-http-experimental"             % "1.0",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental"  % "1.0",

  "org.scalatest"     %% "scalatest"                          % "2.2.1" % "test",
  "com.typesafe.akka" %% "akka-stream-testkit-experimental"   % "1.0"   % "test",
  "com.typesafe.akka" %% "akka-http-testkit-experimental"     % "1.0"   % "test"
)

homepage := Some(url("https://github.com/lancearlaus/akka-streams-http-intro"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

mainClass in (Compile, run) := Some("Main")