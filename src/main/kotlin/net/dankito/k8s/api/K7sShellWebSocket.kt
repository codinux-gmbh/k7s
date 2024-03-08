package net.dankito.k8s.api

import io.fabric8.kubernetes.client.dsl.ExecWatch
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.OnClose
import jakarta.websocket.OnError
import jakarta.websocket.OnMessage
import jakarta.websocket.OnOpen
import jakarta.websocket.Session
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint
import net.codinux.log.logger
import net.dankito.k8s.domain.service.KubernetesService
import java.util.concurrent.ConcurrentHashMap

@ServerEndpoint("/k7s/shell/{namespace}/{podName}")
@ApplicationScoped
class K7sShellWebSocket(
    private val service: KubernetesService
) {

    private val sessions = ConcurrentHashMap<Session, ExecWatch>()

    private val log by logger()

    @OnOpen
    fun onOpen(session: Session, @PathParam("namespace") namespace: String, @PathParam("podName") podName: String) {
        val execWatch = service.openShell(namespace, podName)

        //sessions.put(session, execWatch)

        log.info { "Shell connection for ${session.id} to $namespace/$podName opened" }
    }

    @OnClose
    fun onClose(session: Session, @PathParam("namespace") namespace: String, @PathParam("podName") podName: String) {
        sessions[session]?.let { execWatch ->
            try {
                execWatch.close()
            } catch (e: Throwable) {
                log.error(e) { "Error when closing shell connection to $namespace/$podName" }
            }

            log.info { "Shell connection for ${session.id} to $namespace/$podName closed" }
        }
    }

    @OnError
    fun onError(session: Session, @PathParam("namespace") namespace: String, @PathParam("podName") podName: String, throwable: Throwable) {

    }

    @OnMessage
    fun onMessage(session: Session, message: String, @PathParam("namespace") namespace: String, @PathParam("podName") podName: String) {

    }
}