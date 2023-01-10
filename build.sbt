ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "zio-2-fiber-leak-recreator",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.5",
      "dev.zio" %% "zio-interop-cats" % "23.0.0.0",
      "dev.zio" %% "zio-test" % "2.0.5" % Test,
      "org.http4s" %% "http4s-blaze-client" % "0.23.13",
      "org.typelevel"%% "cats-effect" % "3.4.4"
),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
