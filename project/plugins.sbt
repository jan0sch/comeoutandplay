addSbtPlugin("com.typesafe.sbt"  % "sbt-git"          % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"       % "5.0.0")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"     % "1.5.1")
addSbtPlugin("org.wartremover"   % "sbt-wartremover"  % "2.3.7")
// ScalaJs
//addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "1.0.0-M6")
addSbtPlugin("org.scala-js"       % "sbt-scalajs"              % "0.6.26")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")
addSbtPlugin("com.vmunier"        % "sbt-web-scalajs"          % "1.0.8-0.6")
addSbtPlugin("com.typesafe.sbt"   % "sbt-gzip"                 % "1.0.2")
// Play framework
addSbtPlugin("com.typesafe.play" % "sbt-plugin"       % "2.6.20")
addSbtPlugin("com.typesafe.sbt"  % "sbt-coffeescript" % "1.0.2")
addSbtPlugin("com.typesafe.sbt"  % "sbt-less"         % "1.1.2")
addSbtPlugin("com.typesafe.sbt"  % "sbt-jshint"       % "1.0.6")
addSbtPlugin("com.typesafe.sbt"  % "sbt-rjs"          % "1.0.10")
addSbtPlugin("com.typesafe.sbt"  % "sbt-digest"       % "1.1.4")
addSbtPlugin("com.typesafe.sbt"  % "sbt-mocha"        % "1.1.2")
addSbtPlugin("org.madoushi.sbt"  % "sbt-sass"         % "2.0.0")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.25" // Needed by sbt-git
