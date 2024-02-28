package net.dankito.k8s.api

import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.sse.Sse
import jakarta.ws.rs.sse.SseEventSink
import net.dankito.k8s.api.dto.HomePageData
import net.dankito.k8s.api.dto.ResourceItemsViewData
import net.dankito.k8s.domain.service.KubernetesService
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestStreamElementType
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@Path("")
@Produces(MediaType.TEXT_HTML)
class K7sPage(
    private val service: KubernetesService,
    @Location("home-page") private val homePage: Template,
    @Location("logs-view") private val logsView: Template,
    private val sse: Sse
) {

    @GET
    @Blocking // TODO: why doesn't KubernetesClient work with suspending / non-blocking function?
    fun homePage(): TemplateInstance =
        homePage.data(HomePageData(service.getAllAvailableResourceTypes(), service.podResource, service.getPods()))

    @Path("page/resources/{group}/{name}") // TODO: don't know why, but if i use only "/resources/..." Quarkus cannot resolve the method anymore and i only get 404 Not Found
    @GET
    @Blocking
    fun getResourcesView(
        @RestPath("group") group: String,
        @RestPath("name") name: String,
    ): TemplateInstance {
        val resource = service.getResource(group.takeUnless { it.isBlank() || it == "null" }, name)
        if (resource == null) {
            throw NotFoundException("Resource for group '$group' and name '$name' not found in Kubernetes cluster")
        }

        val resourceItems = service.getResourceItems(resource)

        return homePage.getFragment("resourceItems")
            .data(ResourceItemsViewData(resource, resourceItems))
    }

    @Path("watch/resources/{group}/{name}")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    fun watchResources(
        @RestPath("group") group: String,
        @RestPath("name") name: String,
        @Context sseEventSink: SseEventSink
    ) {
        val resource = service.getResource(group.takeUnless { it.isBlank() || it == "null" }, name)
        if (resource == null) {
            throw NotFoundException("Resource for group '$group' and name '$name' not found in Kubernetes cluster")
        }

        val fragment = homePage.getFragment("resourceItems")

        service.watchResourceItems(resource) { resourceItems ->
            if (sseEventSink.isClosed) {
                return@watchResourceItems // TODO: stop watcher
            } else {
                val html = fragment.data(ResourceItemsViewData(resource, resourceItems)).render()
                sseEventSink.send(sse.newEvent("resourceItemsUpdated", html))
            }
        }
    }


    @Path("logs/{podNamespace}/{podName}") // TODO: there are also other resources that have logs like Deployments, ReplicaSets, ...
    @GET
    @Blocking
    fun getLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String,
        @RestQuery("since") since: String? = null
    ) = getLogs(podNamespace, podName, null, since)

    @Path("logs/{podNamespace}/{podName}/{containerName}")
    @GET
    @Blocking
    fun getLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String,
        @RestPath("containerName") containerName: String? = null,
        @RestQuery("since") since: String? = null
    ): TemplateInstance {
        val sinceTimeUtc = since?.let { ZonedDateTime.parse(it) }
        val startWatchingAt = Instant.now().atZone(ZoneOffset.UTC)

        val logs = service.getLogs(podName, podNamespace, containerName, sinceTimeUtc)

        return logsView
            .data("logs", logs)
            .data("startWatchingAt", startWatchingAt)
    }

    @Path("watch/logs/{podNamespace}/{podName}")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    fun watchLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String,
        @RestQuery("since") since: String? = null,
        @Context sseEventSink: SseEventSink
    ) {
        watchLogs(podNamespace, podName, null, since, sseEventSink)
    }

    @Path("watch/logs/{podNamespace}/{podName}/{containerName}")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun watchLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String,
        @RestPath("containerName") containerName: String? = null,
        @RestQuery("since") since: String? = null,
        @Context sseEventSink: SseEventSink
    ) {
        val sinceTimeUtc = since?.let { ZonedDateTime.parse(it) }

        val inputStream = service.watchLogs(podName, podNamespace, containerName, sinceTimeUtc)

        inputStream.bufferedReader().use { logReader ->
            logReader.forEachLine { line ->
                if (sseEventSink.isClosed) {
                    return@forEachLine
                } else {
                    sseEventSink.send(sse.newEvent(line))
                }
            }
        }
    }

}