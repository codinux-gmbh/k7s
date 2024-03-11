package net.dankito.k8s.api

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import io.quarkus.scheduler.Scheduled
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
import net.dankito.k8s.api.dto.ItemModificationEvent
import net.dankito.k8s.api.dto.ResourceItemRowViewData
import net.dankito.k8s.api.dto.ResourceItemsViewData
import net.dankito.k8s.domain.model.ResourceItem
import net.dankito.k8s.domain.model.WatchAction
import net.dankito.k8s.domain.service.KubernetesService
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery
import org.jboss.resteasy.reactive.RestStreamElementType
import java.io.Closeable
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

@Path("")
@Produces(MediaType.TEXT_HTML)
class K7sPage(
    private val service: KubernetesService,
    @Location("home-page") private val homePage: Template,
    @Location("logs-view") private val logsView: Template,
    private val sse: Sse,
    private val objectMapper: ObjectMapper
) {

    private val resourceWatches = ConcurrentHashMap<SseEventSink, Closeable>()

    @Scheduled(every="1m")
    internal fun cleanWatches() {
        resourceWatches.toMap().forEach { (sseEventSink, closeable) ->
            if (sseEventSink.isClosed) {
                closeable.close()
                resourceWatches.remove(sseEventSink)
            }
        }
    }


    @GET
    @Blocking // TODO: why doesn't KubernetesClient work with suspending / non-blocking function?
    fun homePage(
        @RestQuery("context") context: String? = null,
        @RestQuery("namespace") namespace: String? = null
    ): TemplateInstance {
        val defaultResource = service.getPods(context, namespace)

        return homePage.data(HomePageData(
            service.getAllAvailableResourceTypes(context),
            service.getNamespaces(context)?.items.orEmpty(),
            service.contextsNames,
            service.defaultContext.takeUnless { it == KubernetesService.NonNullDefaultContextName },
            service.podResource,
            defaultResource?.items.orEmpty(),
            context?.takeUnless { it == service.defaultContext },
            namespace,
            defaultResource?.resourceVersion
        ))
    }

    @Path("page/resources/{group}/{name}") // TODO: don't know why, but if i use only "/resources/..." Quarkus cannot resolve the method anymore and i only get 404 Not Found
    @GET
    @Blocking
    fun getResourcesView(
        @RestPath("group") group: String,
        @RestPath("name") name: String,
        @RestQuery("context") context: String? = null,
        @RestQuery("namespace") namespace: String? = null
    ): TemplateInstance {
        val resource = service.getResource(group.takeUnless { it.isBlank() || it == "null" }, name, context)
        if (resource == null) {
            throw NotFoundException("Resource for group '$group' and name '$name' not found in Kubernetes cluster")
        }

        val resourceItems = service.getResourceItems(resource, context, namespace)

        return homePage.getFragment("resourceItems")
            .data(ResourceItemsViewData(resource, resourceItems?.items.orEmpty(), context.takeUnless { it == service.defaultContext }, namespace, resourceItems?.resourceVersion))
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
        @RestQuery("resourceVersion") resourceVersion: String? = null,
        @Context sseEventSink: SseEventSink
    ) {
        val resource = service.getResource(group.takeUnless { it.isBlank() || it == "null" }, name, context)
        if (resource == null) {
            throw NotFoundException("Resource for group '$group' and name '$name' not found in Kubernetes cluster")
        }
        if (resource.isWatchable == false) {
            return // a not watchable resource like Binding, ComponentStatus, NodeMetrics, PodMetrics, ...
        }

        val resourceItemsFragment = homePage.getFragment("resourceItems")
        val resourceItemTableRowFragment = homePage.getFragment("resourceItemTableRow")
        val contextValue = context.takeUnless { it == service.defaultContext }

        val watch = service.watchResourceItems(resource, context, namespace, resourceVersion?.takeUnless { it.isBlank() || it == "null" }) { action, item, insertionIndex ->
            if (sseEventSink.isClosed) {
                true
            } else {
                if (action == WatchAction.Added) {
                    val html = resourceItemTableRowFragment.data(ResourceItemRowViewData(item, resource, namespace)).render()
                    sseEventSink.send(sse.newEvent("resourceItemAdded", createEvent(item, html, insertionIndex)))
                } else if (action == WatchAction.Modified) {
                    val html = resourceItemTableRowFragment.data(ResourceItemRowViewData(item, resource, namespace)).render()
                    sseEventSink.send(sse.newEvent("resourceItemUpdated", createEvent(item, html)))
                } else if (action == WatchAction.Deleted) {
                    sseEventSink.send(sse.newEvent("resourceItemDeleted", createEvent(item)))
                }

                false
            }
        }

        if (watch != null) {
            resourceWatches[sseEventSink] = watch
        }
    }

    private fun createEvent(item: ResourceItem, html: String? = null, insertionIndex: Int? = null): String {
        val event = ItemModificationEvent(item.htmlSafeId, html, insertionIndex)

        return objectMapper.writeValueAsString(event)
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

        // TODO: also close LogWatch
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