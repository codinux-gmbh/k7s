package net.dankito.k8s.domain.config

import io.fabric8.kubernetes.client.Config
import io.fabric8.kubernetes.client.internal.KubeConfigUtils
import io.quarkus.runtime.Startup
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import net.codinux.log.logger
import net.dankito.k8s.domain.model.KubeConfigs
import java.io.File
import java.util.concurrent.TimeUnit

@Singleton
@Startup
class KubernetesConfig {

    private val log by logger()

    @Produces
    fun kubeConfigs(): KubeConfigs {
        if (isRunningInKubernetes() == false) { // loading contexts from kubectl or KUBECONFIG is only senseful when running outside Kubernetes
            // the default config loading mechanism in some cases doesn't load all contexts that kubectl sees, e.g. when KUBECONFIG
            // environment variable is set -> try to get all contexts from kubectl and if kubectl is not installed from KUBECONFIG environment variable
            val kubeCtlConfigs = loadConfigsFromKubectl()
            if (kubeCtlConfigs != null) {
                return kubeCtlConfigs
            }

            val kubeConfigsFromEnvironmentVariable = loadConfigsFromKubeConfigEnvironmentVariable()
            if (kubeConfigsFromEnvironmentVariable != null) {
                return kubeConfigsFromEnvironmentVariable
            }
        }

        val defaultConfig = Config.autoConfigure(null)
        return KubeConfigs(
            defaultConfig.currentContext?.name,
            if (defaultConfig.contexts.isNotEmpty()) {
                mapOf((defaultConfig.currentContext ?: defaultConfig.contexts.first()).name to defaultConfig) // the default context doesn't load other contexts than the current one
            } else {
                emptyMap()
            }
        )
    }

    private fun isRunningInKubernetes(): Boolean {
        try {
            val kubernetesSecretsFolder = File("/var/run/secrets/kubernetes.io/serviceaccount")

            if (kubernetesSecretsFolder.exists()) {
                val files = kubernetesSecretsFolder.list().orEmpty()
                return files.size >= 3 && files.contains("ca.crt") && files.contains("namespace") && files.contains("token")
            }
        } catch (e: Throwable) {
            log.error(e) { "Could not determine if application is running in Kubernetes" }
        }

        return false
    }

    private fun loadConfigsFromKubectl(): KubeConfigs? {
        try {
            val kubectlConfigProcess = Runtime.getRuntime().exec("kubectl config view --flatten=true") // TODO: check if kubectl is installed first
            val kubectlConfigString = kubectlConfigProcess.inputReader().readText()
            kubectlConfigProcess.waitFor(500, TimeUnit.MILLISECONDS)
            val exitCode = kubectlConfigProcess.exitValue()
            if (exitCode == 0) {
                return loadConfigsFromString(kubectlConfigString)
            }
        } catch (e: Throwable) {
            log.info { "Could not load contexts from kubectl (${e.message}), trying to load contexts from KUBECONFIG environment variable" }
            log.debug(e) { "Error when trying to load contexts from kubectl was:" }
        }

        return null
    }

    private fun loadConfigsFromKubeConfigEnvironmentVariable(): KubeConfigs? {
        try {
            val kubeConfigEnvironmentVariable = System.getenv("KUBECONFIG") ?: System.getProperty("KUBECONFIG")

            if (kubeConfigEnvironmentVariable.isNullOrBlank() == false) {
                val configuredConfigs = kubeConfigEnvironmentVariable.split(':').flatMap {
                    val fileOrDirectory = File(it)
                    if (fileOrDirectory.isFile) {
                        listOf(loadConfigsFromString(fileOrDirectory.readText()))
                    } else {
                        fileOrDirectory.listFiles().orEmpty().filter { it.isFile }.map {
                            loadConfigsFromString(it.readText())
                        }
                    }
                }

                val contextConfigs = configuredConfigs.flatMap { it.contextConfigs.entries }.associateBy({ it.key }, { it.value })
                return KubeConfigs(configuredConfigs.firstNotNullOfOrNull { it.defaultContext }, contextConfigs)
            }
        } catch (e: Throwable) {
            log.info(e) { "Could not load contexts from KUBECONFIG environment variable, using default contexts" }
        }

        return null
    }

    private fun loadConfigsFromString(configString: String): KubeConfigs {
        val allContextsConfig = KubeConfigUtils.parseConfigFromString(configString) // load it to get all contexts

        val contextConfigs = allContextsConfig.contexts.map { it.name }
            .associateBy({ it }, { Config.fromKubeconfig(it, configString, null) })

        return KubeConfigs(allContextsConfig.currentContext, contextConfigs)
    }

}