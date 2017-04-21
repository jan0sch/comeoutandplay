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
        library.akkaQuartz,
        library.ficus,
        library.guice,
        library.playBootstrap,
        library.playMailer,
        library.playSil,
        library.playSilBcrypt,
        library.playSilPersist,
        library.playSilJca,
        library.playSlick,
        library.playSlickEvo,
        library.postgresql,
        library.scalaJsScripts,
        library.slickPg,
        library.slickPgDate,
        library.webjarsPlay,
        library.scalaCheck % Test,
        library.scalaTest  % Test,
        cache,
        filters
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
      val akkaQuartz     = "1.5.0-akka-2.4.x"
      val ficus          = "1.2.6"
      val guice          = "4.0.1"
      val playBootstrap  = "1.1.1-P25-B3-SNAPSHOT"
      val playMailer     = "5.0.0"
      val playSil        = "4.0.0"
      val playSlick      = "2.0.2"
      val postgresql     = "42.0.0"
      val scalaCheck     = "1.13.5"
      val scalaJsScr     = "1.0.0"
      val scalaTest      = "3.0.3"
      val slickPg        = "0.14.6"
      val webjarsPlay    = "2.5.0-4"
    }

    val akkaQuartz     = "com.enragedginger"   %% "akka-quartz-scheduler"            % Version.akkaQuartz
    val ficus          = "com.iheart"          %% "ficus"                            % Version.ficus
    val guice          = "net.codingwell"      %% "scala-guice"                      % Version.guice
    val playBootstrap  = "com.adrianhurt"      %% "play-bootstrap"                   % Version.playBootstrap
    val playMailer     = "com.typesafe.play"   %% "play-mailer"                      % Version.playMailer
    val playSil        = "com.mohiva"          %% "play-silhouette"                  % Version.playSil
    val playSilBcrypt  = "com.mohiva"          %% "play-silhouette-password-bcrypt"  % Version.playSil
    val playSilPersist = "com.mohiva"          %% "play-silhouette-persistence"      % Version.playSil
    val playSilJca     = "com.mohiva"          %% "play-silhouette-crypto-jca"       % Version.playSil
    val playSilTestkit = "com.mohiva"          %% "play-silhouette-testkit"          % Version.playSil % "test"
    val playSlick      = "com.typesafe.play"   %% "play-slick"                       % Version.playSlick
    val playSlickEvo   = "com.typesafe.play"   %% "play-slick-evolutions"            % Version.playSlick
    val postgresql     = "org.postgresql"      %  "postgresql"                       % Version.postgresql
    val scalaCheck     = "org.scalacheck"      %% "scalacheck"                       % Version.scalaCheck
    val scalaJsScripts = "com.vmunier"         %% "scalajs-scripts"                  % Version.scalaJsScr
    val scalaTest      = "org.scalatest"       %% "scalatest"                        % Version.scalaTest
    val slickPg        = "com.github.tminglei" %% "slick-pg"                         % Version.slickPg
    val slickPgDate    = "com.github.tminglei" %% "slick-pg_date2"                   % Version.slickPg
    val webjarsPlay    = "org.webjars"         %% "webjars-play"                     % Version.webjarsPlay
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  gitSettings ++
  headerSettings ++
  resolverSettings

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

lazy val jcenter = Resolver.jcenterRepo
lazy val sonatype = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
lazy val resolverSettings =
  Seq(
    externalResolvers := List(jcenter, sonatype)
  )

