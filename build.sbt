
val Http4sVersion = "0.21.4"
val CirceVersion = "0.13.0"
val LogbackVersion = "1.2.3"
val MonixVersion = "3.2.1"
val TypesafeConfigVersion = "1.4.0"
val NewtypeVersion = "0.4.4"
val ScalaTestVersion = "3.1.2"
val SilencerVersion = "1.7.0"
val GeoIP2Version = "2.13.1"
val CommonsCompressVersion = "1.20"
val EHCacheVersion = "3.8.1"

// Used below for parsing versions, specified via git tags
val ReleaseTag = """^v(\d+\.\d+(?:\.\d+(?:[-.]\w+)?)?)$""".r

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(AutomateHeaderPlugin)
  .enablePlugins(SbtTwirl)
  .enablePlugins(DockerPlugin)
  .enablePlugins(GitVersioning)
  .settings(
    organization := "org.alexn",
    name := "http-dope",
    scalaVersion := "2.13.2",

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
      "org.ehcache" % "ehcache" % EHCacheVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    ),

    addCompilerPlugin("org.typelevel"   %% "kind-projector"     % "0.10.3"),
    addCompilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin("com.github.ghik" %  "silencer-plugin"    % SilencerVersion cross CrossVersion.full),

    // Avoids classloader issues
    fork := true,

    organizationName := "Alexandru Nedelcu",
    startYear := Some(2020),
    licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt")),

    herokuAppName in Compile := "http-dope",
    herokuJdkVersion in Compile := "11",

    dockerBaseImage := "adoptopenjdk/openjdk11" ,
    packageName in Docker := "http-dope",

    dockerUpdateLatest := true,
    dockerUsername := Some("alexelcu"),
    dockerRepository := Some("http-dope"),
    dockerAlias := DockerAlias(None, dockerUsername.value, (packageName in Docker).value, git.gitDescribedVersion.value),

    // Twirl template settings
    sourceDirectories in (Compile, TwirlKeys.compileTemplates) := (unmanagedSourceDirectories in Compile).value,
    TwirlKeys.templateImports ++= Seq(
      "httpdope.config._",
      "httpdope.static.html._"
    ),

    // --------------------
    // Versioning setup
    //
    // Uses hash versioning (the sha of the git commit becomes the version suffix).
    // See: https://github.com/sbt/sbt-git
    //
    git.baseVersion := "0.0.1",

    git.gitTagToVersionNumber := {
      case ReleaseTag(v) => Some(v)
      case _ => None
    },
    git.uncommittedSignifier := Some("SNAPSHOT"),
    git.formattedShaVersion := {
      val suffix = git.makeUncommittedSignifierSuffix(git.gitUncommittedChanges.value, git.uncommittedSignifier.value)

      git.gitHeadCommit.value map { _.substring(0, 7) } map { sha =>
        git.baseVersion.value + "-" + sha + suffix
      }
    },
  )

// Reloads build.sbt changes whenever detected
Global / onChangedBuildSource := ReloadOnSourceChanges
