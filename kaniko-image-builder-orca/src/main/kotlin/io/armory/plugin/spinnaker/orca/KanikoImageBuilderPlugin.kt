package io.armory.plugin.spinnaker.orca

import com.netflix.spinnaker.kork.plugins.api.PluginSdks
import com.netflix.spinnaker.orca.api.preconfigured.jobs.PreconfiguredJobConfigurationProvider
import com.netflix.spinnaker.orca.api.preconfigured.jobs.PreconfiguredJobStageProperties
import com.netflix.spinnaker.orca.clouddriver.config.KubernetesPreconfiguredJobProperties
import org.pf4j.Extension
import org.pf4j.Plugin
import org.pf4j.PluginWrapper

class KanikoImageBuilderPlugin(wrapper: PluginWrapper): Plugin(wrapper) {
    override fun start() {
        System.out.println("Armory.KanikoImageBuilderPlugin.start()")
    }

    override fun stop() {
        System.out.println("Armory.KanikoImageBuilderPlugin.stop()")
    }
}

@Extension
class KanikoImageBuilderPreconfiguredJobStage(val pluginSdks: PluginSdks, val config: KanikoImageBuilderConfig): PreconfiguredJobConfigurationProvider {
    override fun getJobConfigurations(): List<out PreconfiguredJobStageProperties> {
        return listOf(KanikoImageBuilderPreconfiguredJobFactory.build(config))
    }
}