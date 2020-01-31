val Http4sVersion = "0.21.0-RC2"
val CirceVersion = "0.13.0-RC1"
val LogbackVersion = "1.2.3"
val MonixVersion = "3.1.0"
val TypesafeConfigVersion = "1.4.0"
val NewtypeVersion = "0.4.3"
val ScalaTestVersion = "3.1.0"
val SilencerVersion = "1.4.4"
val GeoIP2Version = "2.12.0"
val CommonsCompressVersion = "1.19"

lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(SbtTwirl)
  .settings(
    organization := "org.alexn",
    name := "http-dope",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.1",

    scalacOptions ++= Seq(
      // Replaces macro-paradise
      "-Ymacro-annotations",
    ),
    // Disabling the unused import warning, as it's
    // too damn annoying
    scalacOptions --= Seq(
      "-Wunused:imports",
      "-Ywarn-unused:imports",
      "-Ywarn-unused-import",
    ),

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "com.github.ghik" % "silencer-lib" % SilencerVersion  % Provided cross CrossVersion.full,
      "com.maxmind.geoip2" % "geoip2" % GeoIP2Version,
      "com.typesafe" % "config" % TypesafeConfigVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "io.estatico" %% "newtype" % NewtypeVersion,
      "io.monix" %% "monix" % MonixVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-twirl" % Http4sVersion,
      "org.apache.commons" % "commons-compress" % CommonsCompressVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    ),

    addCompilerPlugin("org.typelevel"   %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("com.github.ghik" %  "silencer-plugin"    % SilencerVersion cross CrossVersion.full),

    organizationName := "Alexandru Nedelcu",
    startYear := Some(2020),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),

    herokuAppName in Compile := "http-dope",
    herokuJdkVersion in Compile := "11",

    // Twirl template settings
    sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value,
    TwirlKeys.templateImports ++= Seq(
      "org.alexn.httpdope.config._",
      "org.alexn.httpdope.static.html._"
    )
  )

// Reloads build.sbt changes whenever detected
Global / onChangedBuildSource := ReloadOnSourceChanges
