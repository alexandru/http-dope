val finchVersion            = "0.25.0"
val monixVersion            = "3.0.0-RC2"
val scalatestVersion        = "3.0.5"
val circeVersion            = "0.10.0"
val logbackVersion          = "1.2.3"
val typesafeConfigVersion   = "1.3.2"

lazy val root = (project in file("."))
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(JavaServerAppPackaging)
  .enablePlugins(SystemdPlugin)
  .enablePlugins(DebianPlugin)
  .settings(
    organization := "org.alexn",
    name := "vimeo-download-plus",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.7",

    scalacOptions ++= Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-language:higherKinds",
      "-language:postfixOps",
      "-feature",
      "-Ypartial-unification",
    ),

    libraryDependencies ++= Seq(
      "com.github.finagle" %% "finchx-core"          % finchVersion,
      "io.monix"           %% "monix"                % monixVersion,
      "io.circe"           %% "circe-generic"        % circeVersion,
      "io.circe"           %% "circe-generic-extras" % circeVersion,
      "io.circe"           %% "circe-parser"         % circeVersion,
      "ch.qos.logback"     %  "logback-classic"      % logbackVersion,
      "com.typesafe"       %  "config"               % typesafeConfigVersion
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
      """|Copyright (c) 2018 Alexandru Nedelcu.
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