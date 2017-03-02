

// Define the scala version for the project
val scalaV = "2.11.8"

lazy val frontend = (project in file("frontend")).settings(
  scalaVersion := scalaV,
  scalaJSProjects := Seq(scalaJs),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    "com.typesafe.play"       %% "play-slick"              % "2.0.2",
    "com.typesafe.play"       %% "play-slick-evolutions"   % "2.0.2",
    "com.vmunier"             %% "scalajs-scripts"         % "1.0.0",
    "org.postgresql"           % "postgresql"              % "9.4.1212",
    "org.webjars"             %% "webjars-play"            % "2.5.0",
    "org.webjars"              % "bootstrap"               % "3.3.7"
  )
).enablePlugins(PlayScala).
  dependsOn(sharedJvm)

lazy val scalaJs = (project in file("scalaJs")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1"
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb).
  dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// loads the server project at sbt startup
onLoad in Global := (Command.process("project frontend", _: State)) compose (onLoad in Global).value
