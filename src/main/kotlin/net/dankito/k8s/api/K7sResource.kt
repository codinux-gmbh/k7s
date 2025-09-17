package net.dankito.k8s.api

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import net.dankito.k8s.domain.service.KubernetesService
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery

@Path("/api/v1/resources")
@Produces(MediaType.APPLICATION_JSON)
class K7sResource(
    private val service: KubernetesService
) {

    @GET
    fun getAllAvailableResourceTypes(@RestQuery("context") context: String? = null) =
        service.getAllAvailableResourceTypes(context)

    @GET
    @Path("/{resourceName}/{namespace}/{itemName}/yaml")
    @Produces("application/yaml")
    fun getResourceItemYaml(@RestPath("resourceName") resourceName: String, @RestPath("namespace") namespace: String?, @RestPath("itemName") itemName: String, @RestQuery("context") context: String? = null): String? {
        return service.getResourceItemYaml(resourceName, namespace?.takeUnless { it.isBlank() || it == "null" }, itemName, context)
    }

    @DELETE
    @Path("/{resourceName}/{namespace}/{itemName}") // TODO: why doesn't "resources/{namespace}/{itemName}" work?
    fun deleteResourceItem(@RestPath("resourceName") resourceName: String, @RestPath("namespace") namespace: String?, @RestPath("itemName") itemName: String, @RestQuery("context") context: String? = null) {
        service.deleteResourceItem(resourceName, namespace?.takeUnless { it.isBlank() || it == "null" }, itemName, context)
    }

    @PATCH
    @Path("/{resourceName}/{namespace}/{itemName}")
    fun scaleResourceItem(@RestPath("resourceName") resourceName: String, @RestPath("namespace") namespace: String?, @RestPath("itemName") itemName: String, @RestQuery("context") context: String? = null, @RestQuery("scaleTo") scaleTo: Int? = null) {
        service.scaleResourceItem(resourceName, namespace?.takeUnless { it.isBlank() || it == "null" }, itemName, context, scaleTo)
    }

}