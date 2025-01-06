ThisBuild / scalaVersion           := "2.13.14"
ThisBuild / crossScalaVersions     := Seq("2.12.19", "2.13.14", "3.3.3")
ThisBuild / versionPolicyIntention := Compatibility.BinaryAndSourceCompatible

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
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "1.0.1")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.23.30")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.27")
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.27" % Optional)
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect" % "2.0.0")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.10")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.16" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.30" % Test)
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.41.5" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.27" % Test)
  .settings(libraryDependencies ++= scalaVersion.value.on(2)(kindProjector))

def kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3").cross(CrossVersion.full)
