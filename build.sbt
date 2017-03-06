// *****************************************************************************
// Projects
// *****************************************************************************

lazy val frontend =
  project
    .in(file("frontend"))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt, PlayScala)
    .settings(settings)
    .settings(
      compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
      libraryDependencies ++= Seq(
        library.playSlick,
        library.playSlickEvolutions,
        library.postgresql,
        library.scalaJsScripts,
        library.webjarsBootstrap,
        library.webjarsPlay,
        library.scalaCheck % Test,
        library.scalaTest  % Test
      ),
      pipelineStages in Assets := Seq(scalaJSPipeline),
      pipelineStages := Seq(digest, gzip),
      scalaJSProjects := Seq(client),
      unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
      unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value),
      wartremoverWarnings in (Compile, compile) ++= Warts.unsafe
    )
    .dependsOn(sharedJvm)

lazy val client =
  project
    .in(file("client"))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt, ScalaJSPlugin, ScalaJSWeb)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.1"
      ),
      persistLauncher := true,
      persistLauncher in Test := false
    )
    .dependsOn(sharedJs)

lazy val shared =
  (crossProject.crossType(CrossType.Pure) in file("shared"))
    .settings(settings)
    .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs  = shared.js

// Load the frontend project upon sbt startup.
onLoad in Global := (Command.process("project frontend", _: State)) compose (onLoad in Global).value

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val bootstrap      = "3.3.7"
      val playSlick      = "2.0.2"
      val postgresql     = "42.0.0"
      val scalaCheck     = "1.13.4"
      val scalaJsScripts = "1.0.0"
      val scalaTest      = "3.0.1"
      val webjarsPlay    = "2.5.0-4"
    }
    val playSlick           = "com.typesafe.play" %% "play-slick"              % Version.playSlick
    val playSlickEvolutions = "com.typesafe.play" %% "play-slick-evolutions"   % Version.playSlick
    val postgresql          = "org.postgresql"    %  "postgresql"              % Version.postgresql 
    val scalaCheck          = "org.scalacheck"    %% "scalacheck"              % Version.scalaCheck
    val scalaJsScripts      = "com.vmunier"       %% "scalajs-scripts"         % Version.scalaJsScripts
    val scalaTest           = "org.scalatest"     %% "scalatest"               % Version.scalaTest
    val webjarsBootstrap    = "org.webjars"       %  "bootstrap"               % Version.bootstrap
    val webjarsPlay         = "org.webjars"       %% "webjars-play"            % Version.webjarsPlay
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  gitSettings ++
  headerSettings

lazy val commonSettings =
  Seq(
    scalaVersion in ThisBuild := "2.11.8",
    organization := "de.hoshikuzu",
    licenses += ("AGPLv3",
                 url("https://www.gnu.org/licenses/agpl.html")),
    mappings.in(Compile, packageBin) += baseDirectory.in(ThisBuild).value / "LICENSE" -> "LICENSE",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:_",
      "-target:jvm-1.8",
      "-unchecked",
//      "-Xfatal-warnings",
      "-Xfuture",
      "-Xlint",
      "-Ydelambdafy:method",
      "-Yno-adapted-args",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused-import",
      "-Ywarn-value-discard"
    ),
    javacOptions ++= Seq(
      "-source", "1.8",
      "-target", "1.8"
    )
)

lazy val gitSettings =
  Seq(
    git.useGitDescribe := true
  )

import de.heikoseeberger.sbtheader.HeaderPattern
import de.heikoseeberger.sbtheader.license._
lazy val headerSettings =
  Seq(
    headers := Map("scala" -> AGPLv3("2017", "Jens Grassel & André Schütz"))
  )

