addSbtPlugin("com.typesafe.sbt"  % "sbt-git"          % "0.8.5")
addSbtPlugin("de.heikoseeberger" % "sbt-header"       % "1.8.0")
addSbtPlugin("com.geirsson"      % "sbt-scalafmt"     % "0.6.1")
addSbtPlugin("org.wartremover"   % "sbt-wartremover"  % "2.2.1")
// ScalaJs
addSbtPlugin("org.scala-js"      % "sbt-scalajs"      % "0.6.19")
addSbtPlugin("com.vmunier"       % "sbt-web-scalajs"  % "1.0.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-gzip"         % "1.0.0")
// Play framework
addSbtPlugin("com.typesafe.play" % "sbt-plugin"       % "2.6.3")
addSbtPlugin("com.typesafe.sbt"  % "sbt-coffeescript" % "1.0.1")
addSbtPlugin("com.typesafe.sbt"  % "sbt-less"         % "1.1.0")
addSbtPlugin("com.typesafe.sbt"  % "sbt-jshint"       % "1.0.4")
addSbtPlugin("com.typesafe.sbt"  % "sbt-rjs"          % "1.0.8")
addSbtPlugin("com.typesafe.sbt"  % "sbt-digest"       % "1.1.1")
addSbtPlugin("com.typesafe.sbt"  % "sbt-mocha"        % "1.1.0")
//addSbtPlugin("org.irundaia.sbt"  % "sbt-sassify"      % "1.4.6")
addSbtPlugin("org.madoushi.sbt"  % "sbt-sass"         % "0.9.3")

// there is a compatibility issue with sbt-less and the latest sbt-js-engine - downgrade version until fixed
// https://github.com/sbt/sbt-less/issues/95
dependencyOverrides += "com.typesafe.sbt" % "sbt-js-engine" % "1.1.4"
