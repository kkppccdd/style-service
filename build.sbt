name := """style-service"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.7.play24",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.3",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.10.40",
  "com.amazonaws" % "aws-java-sdk-sqs" % "1.10.40",
  specs2 % Test,
  "org.webjars" % "angularjs" % "1.4.8",
  "org.webjars" % "bootstrap" % "3.3.6"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// use the following for play 2.4
resolvers += "Kaliber Internal Repository" at "https://jars.kaliber.io/artifactory/libs-release-local"


// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

EclipseKeys.withSource := true