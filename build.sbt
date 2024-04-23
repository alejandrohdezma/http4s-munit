ThisBuild / scalaVersion           := "2.13.13"
ThisBuild / crossScalaVersions     := Seq("2.12.19", "2.13.13", "3.3.3")
ThisBuild / versionPolicyIntention := Compatibility.None

ThisBuild / organization := "com.alejandrohdezma"

addCommandAlias("ci-test", "fix --check; versionPolicyCheck; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "versionCheck; github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .dependsOn(`http4s-munit` % "compile->test")
  .settings(libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.23.16")

lazy val `http4s-munit` = module
  .settings(Test / fork := true)
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "0.7.29")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.23.26")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.26")
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.26" % Optional)
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.7")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.6")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.5" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.26" % Test)
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.16" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.26" % Test)
  .settings(libraryDependencies ++= scalaVersion.value.on(2)(kindProjector))

def kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.13.3").cross(CrossVersion.full)
