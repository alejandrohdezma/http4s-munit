ThisBuild / scalaVersion       := "2.13.6"
ThisBuild / crossScalaVersions := Seq("2.12.15", "2.13.6")
ThisBuild / organization       := "com.alejandrohdezma"
ThisBuild / extraCollaborators += Collaborator.github("gutiory")
ThisBuild / Test / testOptions += Tests.Argument(TestFrameworks.MUnit, "+l")

addCommandAlias("ci-test", "fix --check; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .settings(mdocOut := file("."))
  .dependsOn(`http4s-munit-testcontainers` % "compile->test")
  .settings(mdocVariables ++= {
    val all = releases.value.filter(_.isPublished)

    val `0.7.x` = all.filter(_.name.startsWith("v0.7.")).reverse.headOption.map(_.tag.substring(1)).getOrElse("")
    val `0.8.x` = all.filter(_.name.startsWith("v0.8.")).reverse.headOption.map(_.tag.substring(1)).getOrElse("")

    Map("VERSION_021x" -> `0.7.x`, "VERSION_022x" -> `0.8.x`)
  })

lazy val `http4s-munit` = module
  .settings(libraryDependencies += "org.scalameta" %% "munit" % "0.7.29")
  .settings(libraryDependencies += "org.http4s" %% "http4s-client" % "0.23.6")
  .settings(libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.6")
  .settings(libraryDependencies += "org.typelevel" %% "munit-cats-effect-3" % "1.0.6")
  .settings(libraryDependencies += "io.circe" %% "circe-parser" % "0.14.1")
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.6" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.6" % Test)
  .settings(addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full)))

lazy val `http4s-munit-testcontainers` = module
  .dependsOn(`http4s-munit`)
  .settings(libraryDependencies += "com.dimafeng" %% "testcontainers-scala-munit" % "0.39.11")
  .settings(libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.6" % Test)
  .settings(libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.6" % Test)
  .settings(libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6" % Test)
  .settings(libraryDependencies += "io.circe" %% "circe-generic" % "0.14.1" % Test)
