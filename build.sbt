val FinchVersion               = "0.28.0"
val MonixVersion               = "3.0.0-RC2"
val ScalatestVersion           = "3.0.5"
val CirceVersion               = "0.11.1"
val LogbackVersion             = "1.2.3"
val TypesafeConfigVersion      = "1.3.2"
val CatsVersion                = "1.6.0"
val GeoIP2Version              = "2.12.0"
val UADetectorCoreVersion      = "0.9.22"
val UADetectorResourcesVersion = "2014.10"
val ScalaLoggingVersion        = "3.9.2"

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(SystemdPlugin)
  .enablePlugins(DebianPlugin)
  .settings(
    organization := "dev1ro",
    name := "http-dope",
    version := "0.0.1",
    scalaVersion := "2.12.8",

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-feature",
      "-Ypartial-unification",
      // Turns all warnings into errors ;-)
      "-Xfatal-warnings",
      // Enables linter options
      "-Xlint:adapted-args", // warn if an argument list is modified to match the receiver
      "-Xlint:nullary-unit", // warn when nullary methods return Unit
      "-Xlint:nullary-override", // warn when non-nullary `def f()' overrides nullary `def f'
      "-Xlint:infer-any", // warn when a type argument is inferred to be `Any`
      "-Xlint:missing-interpolator", // a string literal appears to be missing an interpolator id
      "-Xlint:doc-detached", // a ScalaDoc comment appears to be detached from its element
      "-Xlint:private-shadow", // a private field (or class parameter) shadows a superclass field
      "-Xlint:type-parameter-shadow", // a local type parameter shadows a type already in scope
      "-Xlint:poly-implicit-overload", // parameterized overloaded implicit methods are not visible as view bounds
      "-Xlint:option-implicit", // Option.apply used implicit view
      "-Xlint:delayedinit-select", // Selecting member of DelayedInit
      "-Xlint:package-object-classes", // Class or object defined in package object
    ),

    libraryDependencies ++= Seq(
      "ch.qos.logback"             %  "logback-classic"      % LogbackVersion,
      "com.github.finagle"         %% "finchx-core"          % FinchVersion,
      "com.maxmind.geoip2"         %  "geoip2"               % GeoIP2Version,
      "com.typesafe"               %  "config"               % TypesafeConfigVersion,
      "com.typesafe.scala-logging" %% "scala-logging"        % ScalaLoggingVersion,
      "io.circe"                   %% "circe-generic"        % CirceVersion,
      "io.circe"                   %% "circe-generic-extras" % CirceVersion,
      "io.circe"                   %% "circe-parser"         % CirceVersion,
      "io.monix"                   %% "monix"                % MonixVersion,
      "net.sf.uadetector"          %  "uadetector-core"      % UADetectorCoreVersion,
      "net.sf.uadetector"          %  "uadetector-resources" % UADetectorResourcesVersion,
      "org.typelevel"              %% "cats-core"            % CatsVersion,
    ),

    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    // Macro enhancements, soon to be integrated in the Scala compiler
    // (in the future 2.13 version)
    addCompilerPlugin(
      // For JSON decoder derivation
      "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
    ),

    // For automatic headers, enabled by sbt-header
    headerLicense := Some(HeaderLicense.Custom(
      """|Copyright (c) 2019 Alexandru Nedelcu.
         |
         |This program is free software: you can redistribute it and/or modify
         |it under the terms of the GNU General Public License as published by
         |the Free Software Foundation, either version 3 of the License, or
         |(at your option) any later version.
         |
         |This program is distributed in the hope that it will be useful,
         |but WITHOUT ANY WARRANTY; without even the implied warranty of
         |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
         |GNU General Public License for more details.
         |
         |You should have received a copy of the GNU General Public License
         |along with this program. If not, see <http://www.gnu.org/licenses/>."""
        .stripMargin)),

    // Needed for packaging
    maintainer := "Alexandru Nedelcu <noreply@alexn.org>",
    packageSummary := "Vimeo Download Plus",
    packageDescription := "A server for discovering and redirecting to Vimeo raw video files",
    debianPackageDependencies := Seq("openjdk-8-jdk"),

    // For Heroku deployment
    herokuAppName in Compile := "vimeo-downloads-plus"
  )
