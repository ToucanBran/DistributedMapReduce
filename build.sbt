val akkaVersion = "2.5.12"
val constructrVersion = "0.19.0"

lazy val commonSettings = Seq(
        scalaVersion := "2.12.5",
        version := "1.0",
        resolvers += Resolver.bintrayRepo("hseeberger", "maven"),
        scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")
    )

lazy val akkaSettings = Seq(
        libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.typesafe.akka" %% "akka-remote" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
        "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
        "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
        "de.heikoseeberger" %% "constructr" % constructrVersion,
        "com.lightbend.constructr"  %% "constructr-coordination-zookeeper"  % "0.3.3",
        "de.heikoseeberger" %% "constructr-coordination-etcd" % constructrVersion,
        "ch.qos.logback" % "logback-classic" % "1.2.3")
    )

lazy val cloudClient = (project in file("CloudClient"))
  .settings(
    name := "CloudClient",
    mainClass in Compile := Some("client.CloudClientOneMaster"),
    packageName in Docker := name.value + "-docker",
    fork in run := true
  )
  .settings(akkaSettings)
   .settings(commonSettings)
   .settings(dockerCommon)
   .dependsOn(common)
   .enablePlugins(JavaAppPackaging)

lazy val mapReduce = (project in file("MapReduce"))
  .settings(
    name := "MapReduce",
    mainClass in Compile := Some("cloud.CloudMasterActor"),
    packageName in Docker := name.value + "-docker",
    fork in run := true
  )
  .settings(akkaSettings)
   .settings(commonSettings)
   .settings(dockerCommon)
   .dependsOn(common)
   .enablePlugins(JavaAppPackaging)

lazy val common = Project("common", file("Common"))
  .settings(
    name := "Common"
  )
  .settings(commonSettings)
  .settings(akkaSettings)

lazy val dockerCommon = Seq (
    dockerBaseImage := "java",
    dockerRepository := Some("bgomez89"),
    packageName in Docker := name.value + "-docker",
    dockerUpdateLatest := true)

lazy val docker = (project in file("."))
.settings(commonSettings)
.aggregate(cloudClient, mapReduce)