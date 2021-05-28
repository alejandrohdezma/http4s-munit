ThisBuild / scalaVersion             := "2.13.4"
ThisBuild / crossScalaVersions       := Seq("2.12.12", "2.13.4")
ThisBuild / organization             := "com.alejandrohdezma"
ThisBuild / extraCollaborators       += Collaborator.github("gutiory")
ThisBuild / testFrameworks           += new TestFramework("munit.Framework")
ThisBuild / Test / parallelExecution := false

addCommandAlias("ci-test", "fix --check; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))

lazy val `http4s-munit` = module
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "0.7.26")
  .settings(libraryDependencies += "org.http4s" %% "http4s-async-http-client" % "0.21.24")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.21.24")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.21.24")
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect-2" % "1.0.3")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.21.24" % Test)
  .settings(libraryDependencies += "org.typelevel" %% "mouse" % "1.0.3" % Test)
  .settings(addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.0" cross CrossVersion.full))

lazy val `http4s-munit-testcontainers` = module
  .dependsOn(`http4s-munit`)
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.39.5")
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.21.24" % Test)
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % Test)
  .settings(libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1" % Test)
