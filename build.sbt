val Scala3 = "3.3.0" // scala-steward:off
ThisBuild / scalaVersion       := "2.13.11"
ThisBuild / crossScalaVersions := Seq("2.12.18", "2.13.11", Scala3)

ThisBuild / organization := "com.alejandrohdezma"

addCommandAlias("ci-test", "scalafmtCheckAll; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .dependsOn(`http4s-munit` % "compile->test")
  .settings(scalacOptions -= "-Wnonunit-statement")
  .settings(libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.23.15")

lazy val `http4s-munit` = module
  .settings(Test / fork := true)
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "0.7.29")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.23.23")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.23")
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.23" % Optional)
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.7")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.6")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.11" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.23" % Test)
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.16" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.23" % Test)
  .settings(
    libraryDependencies ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, _) => compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full) }
      .toList
  )
