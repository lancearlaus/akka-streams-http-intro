name := "akka-streams-http-presentation"

organization := "com.lancearlaus"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0",
  "com.typesafe.akka" %% "akka-http-experimental"   % "1.0",
  "org.scalatest"     %% "scalatest"                % "2.2.1" % "test"
)

homepage := Some(url("https://github.com/lancearlaus/akka-streams-http-presentation"))

licenses := Seq("Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))