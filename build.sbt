name := """play-java-docker-k8s-starter-example"""

version := "1.0-SNAPSHOT"

lazy val root = project
   .in(file("."))
   .enablePlugins(PlayJava)
   .enablePlugins(JavaAppPackaging)

val redisVersion = "2.1.1"

scalaVersion := "2.12.2"

libraryDependencies ++= Seq(guice,
   play.sbt.PlayImport.cacheApi,
   "com.github.karelcemus" %% "play-redis" % redisVersion
)


