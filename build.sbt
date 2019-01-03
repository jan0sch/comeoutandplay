// *****************************************************************************
// Projects
// *****************************************************************************

// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}

lazy val frontend =
  project
    .in(file("frontend"))
    .enablePlugins(
      AutomateHeaderPlugin,
      GitVersioning,
      GitBranchPrompt,
      PlayScala,
      SbtWeb
    )
    .settings(settings)
    .settings(
      name := "frontend",
      compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
      libraryDependencies ++= Seq(
        library.akka,
        library.akkaQuartz,
        library.catsCore,
        library.circeCore,
        library.circeGeneric,
        library.circeParser,
        library.ficus,
        library.guice,
        library.playBootstrap,
        library.playMailer,
        library.playMailerGuice,
        library.playSil,
        library.playSilBcrypt,
        library.playSilPersist,
        library.playSilJca,
        library.playSlick,
        library.playSlickEvo,
        library.postgresql,
        library.scalaJsScripts,
        library.slickPg,
        library.slickPgCirce,
        library.webjarsBoot,
        library.webjarsPlay,
        library.akkaTestkit   % Test,
        library.scalaCheck    % Test,
        library.scalaTest     % Test,
        library.scalaTestPlus % Test,
        ehcache,
        filters,
        guice
      ),
      Assets / pipelineStages := Seq(scalaJSPipeline),
      pipelineStages := Seq(digest, gzip),
      scalaJSProjects := Seq(seabattleClient),
      Compile / unmanagedSourceDirectories := Seq(scalaSource.in(Compile).value),
      Test / unmanagedSourceDirectories := Seq(scalaSource.in(Test).value),
      scalacOptions --= Seq(
        "-Ywarn-unused:imports" // Avoid lots of warnings when Twirl templates are compiled.
      ),
      Compile / compile / wartremoverWarnings := Warts.unsafe.filterNot(Seq(Wart.DefaultArguments, Wart.Any).contains),
      wartremoverExcluded += sourceDirectory.value / "conf" / "routes",
      wartremoverExcluded ++= routes.in(Compile).value
    )
    .dependsOn(seabattleServer, seabattleClient, sharedJvm, sharedJs)

lazy val seabattleServer =
  project
    .in(file("seabattle/server"))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt)
    .settings(settings)
    .settings(
      name := "seabattle-jvm",
      libraryDependencies ++= Seq(
        library.catsCore,
        library.circeCore,
        library.circeGeneric,
        library.circeParser,
        library.scalaCheck % Test,
        library.scalaTest  % Test
      ),
      unmanagedSourceDirectories.in(Compile) := Seq(scalaSource.in(Compile).value),
      unmanagedSourceDirectories.in(Test) := Seq(scalaSource.in(Test).value)
    )
    .dependsOn(sharedJvm)

lazy val seabattleClient =
  project
    .in(file("seabattle/client"))
    .enablePlugins(AutomateHeaderPlugin, GitVersioning, GitBranchPrompt, ScalaJSPlugin, ScalaJSWeb)
    .settings(settings)
    .settings(
      name := "seabattle-client",
      libraryDependencies ++= Seq(
        "org.scala-js"     %%% "scalajs-dom" % "0.9.6"
      ),
      scalaJSUseMainModuleInitializer := false,
      scalaJSUseMainModuleInitializer in Test := false
    )
    .dependsOn(sharedJs)

lazy val seabattleShared =
  crossProject(JSPlatform, JVMPlatform)
    .crossType(CrossType.Pure)
    .in(file("seabattle/shared"))
    .settings(settings)
    .settings(
      name := "seabattle-shared",
      libraryDependencies ++= Seq(
        "org.typelevel"  %%% "cats-core"     % "1.4.0",
        "io.circe"       %%% "circe-core"    % "0.10.0",
        "io.circe"       %%% "circe-generic" % "0.10.0",
        "io.circe"       %%% "circe-parser"  % "0.10.0",
        "org.scalacheck" %%% "scalacheck"    % "1.14.0" % Test,
        "org.scalatest"  %%% "scalatest"     % "3.0.5"  % Test
      )
    )
    .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = seabattleShared.jvm
lazy val sharedJs  = seabattleShared.js

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val akka           = "2.5.17"
      val akkaQuartz     = "1.6.1-akka-2.5.x"
      val cats           = "1.4.0"
      val circe          = "0.10.0"
      val ficus          = "1.4.2"
      val guice          = "4.1.0"
      val playBootstrap  = "1.2-P26-B3"
      val playMailer     = "6.0.1"
      val playSil        = "5.0.0"
      val playSlick      = "3.0.3"
      val postgresql     = "42.2.5"
      val scalaCheck     = "1.14.0"
      val scalaJsScr     = "1.1.2"
      val scalaTest      = "3.0.5"
      val scalaTestPlus  = "3.1.2"
      val slickPg        = "0.16.3"
      val webjarsBoot    = "3.3.7"
      val webjarsPlay    = "2.6.2"
    }

    val akka           = "com.typesafe.akka"          %% "akka-actor"                       % Version.akka
    val akkaTestkit    = "com.typesafe.akka"          %% "akka-testkit"                     % Version.akka
    val akkaQuartz     = "com.enragedginger"          %% "akka-quartz-scheduler"            % Version.akkaQuartz
    val catsCore       = "org.typelevel"              %% "cats-core"                        % Version.cats
    val circeCore      = "io.circe"                   %% "circe-core"                       % Version.circe
    val circeGeneric   = "io.circe"                   %% "circe-generic"                    % Version.circe
    val circeParser    = "io.circe"                   %% "circe-parser"                     % Version.circe
    val ficus          = "com.iheart"                 %% "ficus"                            % Version.ficus
    val guice          = "net.codingwell"             %% "scala-guice"                      % Version.guice
    val playBootstrap  = "com.adrianhurt"             %% "play-bootstrap"                   % Version.playBootstrap
    val playMailer     = "com.typesafe.play"          %% "play-mailer"                      % Version.playMailer
    val playMailerGuice= "com.typesafe.play"          %% "play-mailer-guice"                % Version.playMailer
    val playSil        = "com.mohiva"                 %% "play-silhouette"                  % Version.playSil
    val playSilBcrypt  = "com.mohiva"                 %% "play-silhouette-password-bcrypt"  % Version.playSil
    val playSilPersist = "com.mohiva"                 %% "play-silhouette-persistence"      % Version.playSil
    val playSilJca     = "com.mohiva"                 %% "play-silhouette-crypto-jca"       % Version.playSil
    val playSilTestkit = "com.mohiva"                 %% "play-silhouette-testkit"          % Version.playSil % "test"
    val playSlick      = "com.typesafe.play"          %% "play-slick"                       % Version.playSlick
    val playSlickEvo   = "com.typesafe.play"          %% "play-slick-evolutions"            % Version.playSlick
    val postgresql     = "org.postgresql"             %  "postgresql"                       % Version.postgresql
    val scalaCheck     = "org.scalacheck"             %% "scalacheck"                       % Version.scalaCheck
    val scalaJsScripts = "com.vmunier"                %% "scalajs-scripts"                  % Version.scalaJsScr
    val scalaTest      = "org.scalatest"              %% "scalatest"                        % Version.scalaTest
    val scalaTestPlus  = "org.scalatestplus.play"     %% "scalatestplus-play"               % Version.scalaTestPlus
    val slickPg        = "com.github.tminglei"        %% "slick-pg"                         % Version.slickPg
    val slickPgCirce   = "com.github.tminglei"        %% "slick-pg_circe-json"              % Version.slickPg
    val webjarsBoot    = "org.webjars"                %  "bootstrap"                        % Version.webjarsBoot
    val webjarsPlay    = "org.webjars"                %% "webjars-play"                     % Version.webjarsPlay
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
  gitSettings ++
  headerSettings ++
  resolverSettings ++
  scalafmtSettings

lazy val commonSettings =
  Seq(
    scalaVersion in ThisBuild := "2.12.8",
    organization := "de.hoshikuzu",
    licenses += ("AGPLv3",
                 url("https://www.gnu.org/licenses/agpl.html")),
    mappings.in(Compile, packageBin) += baseDirectory.in(ThisBuild).value / "LICENSE" -> "LICENSE",
    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-explaintypes",
      "-feature",
      "-language:_",
      "-target:jvm-1.8",
      "-unchecked",
      "-Xcheckinit",
      "-Xfuture",
      "-Xlint:adapted-args",
      "-Xlint:by-name-right-associative",
      "-Xlint:constant",
      "-Xlint:delayedinit-select",
      "-Xlint:doc-detached",
      "-Xlint:inaccessible",
      "-Xlint:infer-any",
      "-Xlint:missing-interpolator",
      "-Xlint:nullary-override",
      "-Xlint:nullary-unit",
      "-Xlint:option-implicit",
      "-Xlint:package-object-classes",
      "-Xlint:poly-implicit-overload",
      "-Xlint:private-shadow",
      "-Xlint:stars-align",
      "-Xlint:type-parameter-shadow",
      "-Xlint:unsound-match",
      "-Ydelambdafy:method",
      "-Yno-adapted-args",
      "-Ypartial-unification",
      "-Ywarn-dead-code",
      "-Ywarn-extra-implicit",
      "-Ywarn-inaccessible",
      "-Ywarn-infer-any",
      "-Ywarn-nullary-override",
      "-Ywarn-nullary-unit",
      "-Ywarn-numeric-widen",
      "-Ywarn-unused:implicits",
      "-Ywarn-unused:imports",
      "-Ywarn-unused:locals",
      "-Ywarn-unused:params",
      "-Ywarn-unused:patvars",
      "-Ywarn-unused:privates",
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

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )
lazy val headerSettings =
  Seq(
    headerLicense := Some(HeaderLicense.AGPLv3("2017", "Jens Grassel & André Schütz"))
  )

lazy val jcenter = Resolver.jcenterRepo
lazy val sonatype = "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
lazy val resolverSettings =
  Seq(
    externalResolvers := List(jcenter, sonatype)
  )

