ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "zio-2-fiber-leak-recreator",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.15",
      "dev.zio" %% "zio-interop-cats" % "13.0.0.1",
      "dev.zio" %% "zio-test" % "1.0.15" % Test,
      "org.http4s" %% "http4s-blaze-client" % "0.23.12"
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
