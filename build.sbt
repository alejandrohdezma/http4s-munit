ThisBuild / scalaVersion           := "2.13.18"
ThisBuild / crossScalaVersions     := Seq("2.12.21", "2.13.18", "3.3.7")
ThisBuild / versionPolicyIntention := Compatibility.BinaryCompatible

ThisBuild / organization := "com.alejandrohdezma"

addCommandAlias("ci-test", "fix --check; versionPolicyCheck; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "versionCheck; github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .dependsOn(`http4s-munit` % "compile->test")
  .settings(libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.23.17")

lazy val `http4s-munit` = module
  .settings(Test / fork := true)
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "1.0.4")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.23.33")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.33")
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.33" % Optional)
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect" % "2.1.0")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.15")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.26" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.33" % Test)
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.41.8" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.33" % Test)
  .settings(libraryDependencies ++= scalaVersion.value.on(2)(kindProjector))

def kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.13.4").cross(CrossVersion.full)
