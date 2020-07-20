package io.armory.plugin.spinnaker.orca

class KanikoImageBuilderConfig(
  val application: String,
  val credentials: String,
  val artifactAccount: String,
  val dockerCredentialsSecret: String,
  val artifactServiceUrl: String = "http://spin-clouddriver:7002",
  val builderImage: String = "gcr.io/kaniko-project/executor:latest",
  val namespace: String = "spinnaker",
) {
}