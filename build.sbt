ThisBuild / scalaVersion       := "2.13.8"
ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.8", "3.1.2")
ThisBuild / organization       := "com.alejandrohdezma"
ThisBuild / extraCollaborators += Collaborator.github("gutiory")

addCommandAlias("ci-test", "scalafmtCheckAll; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .dependsOn(`http4s-munit` % "compile->test")
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.40.8")
  .settings(libraryDependencies += "org.http4s" %% "http4s-blaze-client" % "0.23.12")

lazy val `http4s-munit` = module
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "0.7.29")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.23.12")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.12")
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.12" % Optional)
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.7")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.2")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.12" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.12" % Test)
  .settings(
    libraryDependencies ++= CrossVersion
      .partialVersion(scalaVersion.value)
      .collect { case (2, _) => compilerPlugin("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full) }
      .toList
  )
