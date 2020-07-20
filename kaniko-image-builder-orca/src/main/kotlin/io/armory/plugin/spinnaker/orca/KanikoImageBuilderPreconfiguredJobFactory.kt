package io.armory.plugin.spinnaker.orca

import com.netflix.spinnaker.orca.api.preconfigured.jobs.PreconfiguredJobStageParameter
import com.netflix.spinnaker.orca.clouddriver.config.KubernetesPreconfiguredJobProperties
import io.kubernetes.client.models.*

class KanikoImageBuilderPreconfiguredJobFactory {

  companion object {
    fun build(
      config: KanikoImageBuilderConfig
    ): KubernetesPreconfiguredJobProperties {
      var properties = KubernetesPreconfiguredJobProperties()

      properties.label = "Kaniko"
      properties.description = "Build a Docker image using Kaniko"
      properties.type = "kanikoBuild"
      properties.isWaitForCompletion = true
      properties.cloudProvider = "kubernetes"

      properties.credentials = config.credentials
      properties.account = config.credentials
      properties.application = config.application

      // the mapping values are explicitly tied to the order the pod environment
      // variables are defined in. if you change the order of one of those values
      // you should change the mapping order here
      properties.parameters = arrayListOf(
        parameter("REPOSITORY", "Git Repository URL", "manifest.spec.template.spec.initContainers[0].env[1].value"),
        parameter("BRANCH", "Branch or SHA", "manifest.spec.template.spec.initContainers[0].env[0].value"),
        parameter("DOCKERFILE", "Path to Dockerfile", "manifest.spec.template.spec.containers[0].env[0].value"),
        parameter("TARGET_IMAGE", "Image Name", "manifest.spec.template.spec.containers[0].env[2].value")
      )

      properties.manifest = jobManifest(
        config.namespace,
        config.builderImage,
        config.artifactAccount,
        config.artifactServiceUrl,
        config.dockerCredentialsSecret,
      )

      return properties
    }

    fun parameter(name: String, label: String, mapping: String): PreconfiguredJobStageParameter {
      var parameter = PreconfiguredJobStageParameter()
      parameter.name =  name
      parameter.label = label
      parameter.mapping = mapping
      return parameter
    }

    fun jobManifest(
      namespace: String,
      builderImage: String,
      artifactAccount: String,
      artifactServiceUrl: String,
      dockerCredentialsSecretName: String
    ): V1Job {
      var initContainer = V1Container()
        .name("git")
        .image("ethanfrogers/fetch-artifact:v2")
        .env(listOf(
          V1EnvVar().name("BRANCH").value("master"),
          V1EnvVar().name("URL").value("fake-value"),
          V1EnvVar().name("OUTPUT_DIR").value("/workspace"),
          V1EnvVar().name("ARTIFACT_ACCOUNT").value(artifactAccount),
          V1EnvVar().name("ARTIFACT_SERVICE").value(artifactServiceUrl)
        ))
        .volumeMounts(listOf(V1VolumeMount().name("workspace").mountPath("/workspace")))

      var container = V1Container()
        .name("kaniko")
        .image(builderImage)
        .args(listOf(
          "--dockerfile=$(DOCKERFILE)",
          "--context=$(CONTEXT)",
          "--destination=$(DESTINATION)"
        ))
        .env(listOf(
          V1EnvVar().name("DOCKERFILE").value("Dockerfile"),
          V1EnvVar().name("CONTEXT").value("dir:///workspace"),
          V1EnvVar().name("DESTINATION").value("fake-value")
        ))
        .volumeMounts(listOf(
          V1VolumeMount().name("workspace").mountPath("/workspace"),
          V1VolumeMount().name("docker-config").mountPath("/kaniko/.docker")
        ))

      var job = V1JobBuilder()

      job.withMetadata(V1ObjectMeta().generateName("kaniko-builder").namespace(namespace))
        .withSpec(
          V1JobSpec().backoffLimit(0)
            .template(V1PodTemplateSpec().spec(
              V1PodSpec().restartPolicy("Never")
                .initContainers(listOf(initContainer))
                .containers(listOf(container))
                .volumes(listOf(
                  V1Volume().name("workspace").emptyDir(V1EmptyDirVolumeSource()),
                  V1Volume().name("docker-config").secret(V1SecretVolumeSource().secretName(dockerCredentialsSecretName))
                ))
            ))
        )

      return job.build()
    }
  }
}