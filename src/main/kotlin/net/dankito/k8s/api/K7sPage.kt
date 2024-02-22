package net.dankito.k8s.api

import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.dankito.k8s.api.dto.HomePageData
import net.dankito.k8s.domain.service.KubernetesService
import org.jboss.resteasy.reactive.RestPath

@Path("")
@Produces(MediaType.TEXT_HTML)
class K7sPage(
    private val service: KubernetesService,
    @Location("home-page") private val homePage: Template
) {

    @GET
    @Blocking // TODO: why doesn't KubernetesClient work with suspending / non-blocking function?
    fun homePage(): TemplateInstance =
        homePage.data(HomePageData(service.getAllAvailableResourceTypes(), service.getPods()))

    @Path("page/resources/{group}/{name}/{version}") // TODO: don't know why, but if i use only "/resources/..." Quarkus cannot resolve the method anymore and i only get 404 Not Found
    @GET
    @Blocking
    fun getResourcesView(
        @RestPath("group") group: String,
        @RestPath("name") name: String,
        @RestPath("version") version: String
    ): TemplateInstance {
        val resourceItems = service.getResourceItems(group.takeUnless { it.isBlank() || it == "null" }, name, version)

        return homePage.getFragment("resourceItems")
            .data("resourceItems", HomePageData.sort(resourceItems))
    }

}