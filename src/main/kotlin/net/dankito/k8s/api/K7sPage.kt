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
    fun homePage(
        @RestQuery("context") context: String? = null
    ): TemplateInstance =
        homePage.data(HomePageData(
            service.getAllAvailableResourceTypes(context),
            service.getNamespaces(context),
            service.contextsNames,
            service.defaultContext,
            service.podResource,
            service.getPods(context),
            context?.takeUnless { it == service.defaultContext }
        ))

    @Path("page/resources/{group}/{name}") // TODO: don't know why, but if i use only "/resources/..." Quarkus cannot resolve the method anymore and i only get 404 Not Found
    @GET
    @Blocking
    fun getResourcesView(
        @RestPath("group") group: String,
        @RestPath("name") name: String,
        @RestQuery("context") context: String? = null,
        @RestQuery("namespace") namespace: String? = null,
    ): TemplateInstance {
        val resource = service.getResource(group.takeUnless { it.isBlank() || it == "null" }, name, context)
        if (resource == null) {
            throw NotFoundException("Resource for group '$group' and name '$name' not found in Kubernetes cluster")
        }

        val resourceItems = service.getResourceItems(resource, context, namespace)

        return homePage.getFragment("resourceItems")
            .data(ResourceItemsViewData(resource, resourceItems, context.takeUnless { it == service.defaultContext }, namespace))
    }

    @Path("watch/resources/{group}/{name}")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    fun watchResources(
        @RestPath("group") group: String,
        @RestPath("name") name: String,
        @RestQuery("context") context: String? = null,
        @RestQuery("namespace") namespace: String? = null,
        @Context sseEventSink: SseEventSink
    ) {
        val resource = service.getResource(group.takeUnless { it.isBlank() || it == "null" }, name)
        if (resource == null) {
            throw NotFoundException("Resource for group '$group' and name '$name' not found in Kubernetes cluster")
        }
        if (resource.isWatchable == false) {
            return // a not watchable resource like Binding, ComponentStatus, NodeMetrics, PodMetrics, ...
        }

        val fragment = homePage.getFragment("resourceItems")

        service.watchResourceItems(resource, namespace) { resourceItems ->
            if (sseEventSink.isClosed) {
                return@watchResourceItems // TODO: stop watcher
            } else {
                val html = fragment.data(ResourceItemsViewData(resource, resourceItems, context.takeUnless { it == service.defaultContext }, namespace)).render()
                sseEventSink.send(sse.newEvent("resourceItemsUpdated", html))
            }
        }
    }


    @Path("logs/{resourceKind}/{namespace}/{itemName}")
    @GET
    @Blocking
    fun getLogs(
        @RestPath("resourceKind") resourceKind: String,
        @RestPath("namespace") namespace: String,
        @RestPath("itemName") itemName: String,
        @RestQuery("containerName") containerName: String? = null,
        @RestQuery("context") context: String? = null,
        @RestQuery("since") since: String? = null
    ): TemplateInstance {
        val sinceTimeUtc = since?.let { ZonedDateTime.parse(it) }
        val startWatchingAt = Instant.now().atZone(ZoneOffset.UTC)

        val logs = service.getLogs(resourceKind, namespace, itemName, containerName, context, sinceTimeUtc)

        return logsView
            .data("logs", logs)
            .data("startWatchingAt", startWatchingAt)
    }

    @Path("watch/logs/{resourceKind}/{namespace}/{itemName}")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    fun watchLogs(
        @RestPath("resourceKind") resourceKind: String,
        @RestPath("namespace") namespace: String,
        @RestPath("itemName") itemName: String,
        @RestQuery("containerName") containerName: String? = null,
        @RestQuery("context") context: String? = null,
        @RestQuery("since") since: String? = null,
        @Context sseEventSink: SseEventSink
    ) {
        val sinceTimeUtc = since?.let { ZonedDateTime.parse(it) }

        val inputStream = service.watchLogs(resourceKind, namespace, itemName, containerName, context, sinceTimeUtc)

        inputStream?.bufferedReader()?.use { logReader ->
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