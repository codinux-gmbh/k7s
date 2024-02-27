package net.dankito.k8s.domain.service

import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import net.codinux.log.Logger

class KubernetesResourceWatcher<T>(
    private val onCloseLogMessage: String? = null,
    private val log: Logger? = null,
    private val event: (Watcher.Action, T) -> Unit
) : Watcher<T> {

    override fun eventReceived(action: Watcher.Action, resource: T) {
        event(action, resource)
    }

    override fun onClose(cause: WatcherException?) {
        onCloseLogMessage?.let { message ->
            log?.info(cause) { message }
        }
    }

}