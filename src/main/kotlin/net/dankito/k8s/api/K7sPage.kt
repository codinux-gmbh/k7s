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
import org.jboss.resteasy.reactive.RestStreamElementType

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

    @Path("page/resources/{group}/{name}/{version}") // TODO: don't know why, but if i use only "/resources/..." Quarkus cannot resolve the method anymore and i only get 404 Not Found
    @GET
    @Blocking
    fun getResourcesView(
        @RestPath("group") group: String,
        @RestPath("name") name: String,
        @RestPath("version") version: String
    ): TemplateInstance {
        val resource = service.getResource(group.takeUnless { it.isBlank() || it == "null" }, name, version)
        if (resource == null) {
            throw NotFoundException("Resource for group '$group', name '$name' and version '$version' not found in Kubernetes cluster")
        }

        val resourceItems = service.getResourceItems(resource)

        return homePage.getFragment("resourceItems")
            .data(ResourceItemsViewData(resource, resourceItems))
    }

    @Path("logs/{podNamespace}/{podName}") // TODO: there are also other resources that have logs like Deployments, ReplicaSets, ...
    @GET
    @Blocking
    fun getLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String
    ) = getLogs(podNamespace, podName, null)

    @Path("logs/{podNamespace}/{podName}/{containerName}")
    @GET
    @Blocking
    fun getLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String,
        @RestPath("containerName") containerName: String? = null
    ): TemplateInstance {
        val logs = service.getLogs(podName, podNamespace, containerName)

        return logsView.data("logs", logs)
    }

    @Path("watchLogs/{podNamespace}/{podName}")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    fun watchLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String,
        @Context sseEventSink: SseEventSink
    ) {
        watchLogs(podNamespace, podName, null, sseEventSink)
    }

    @Path("watchLogs/{podNamespace}/{podName}/{containerName}")
    @GET
    @Blocking
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_HTML)
    fun watchLogs(
        @RestPath("podNamespace") podNamespace: String,
        @RestPath("podName") podName: String,
        @RestPath("containerName") containerName: String? = null,
        @Context sseEventSink: SseEventSink
    ) {
        val inputStream = service.watchLogs(podName, podNamespace, containerName)
        val template = logsView.getFragment("logEntryRow")

        inputStream.bufferedReader().use { logReader ->
            logReader.forEachLine { line ->
                if (sseEventSink.isClosed) {
                    return@forEachLine
                } else {
                    sseEventSink.send(sse.newEvent(template.data("entry", line).render()))
                }
            }
        }
    }

}