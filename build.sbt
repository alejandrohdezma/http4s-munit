val Scala3 = "3.1.3" // scala-steward:off
ThisBuild / scalaVersion       := "2.13.10"
ThisBuild / crossScalaVersions := Seq("2.12.17", "2.13.10", Scala3)

ThisBuild / organization := "com.alejandrohdezma"

addCommandAlias("ci-test", "scalafmtCheckAll; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .dependsOn(`http4s-munit` % "compile->test")
  .settings(libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.23.13")

lazy val `http4s-munit` = module
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "0.7.29")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.23.17")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.17")
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.16" % Optional)
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.7")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.3")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.5" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.17" % Test)
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.12" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.16" % Test)
  .settings(
    libraryDependencies ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, _) => compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full) }
      .toList
  )
