ThisBuild / versionPolicyIntention := Compatibility.None

ThisBuild / organization := "com.alejandrohdezma"

addCommandAlias("ci-test", "fix --check; versionPolicyCheck; mdoc; +test")
addCommandAlias("ci-docs", "github; headerCreateAll; mdoc")
addCommandAlias("ci-publish", "versionCheck; github; ci-release")

lazy val documentation = project
  .enablePlugins(MdocPlugin)
  .dependsOn(`http4s-munit` % "compile->test")

lazy val `http4s-munit` = module
  .settings(Test / fork := true)
  .settings(libraryDependencies ++= scalaVersion.value.on(2)(kindProjector))

def kindProjector = compilerPlugin("org.typelevel" % "kind-projector" % "0.13.4").cross(CrossVersion.full)
