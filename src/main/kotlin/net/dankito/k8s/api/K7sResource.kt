package net.dankito.k8s.api

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import net.dankito.k8s.domain.service.KubernetesService
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery

@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
class K7sResource(
    private val service: KubernetesService
) {

    @GET
    @Path("resources")
    fun getAllAvailableResourceTypes(@RestQuery("context") context: String? = null) =
        service.getAllAvailableResourceTypes(context)

    @GET
    @Path("resources/{resourceName}/{namespace}/{itemName}/yaml")
    @Produces("application/yaml")
    fun getResourceItemYaml(@RestPath("resourceName") resourceName: String, @RestPath("namespace") namespace: String?, @RestPath("itemName") itemName: String, @RestQuery("context") context: String? = null): String? {
        return service.getResourceItemYaml(resourceName, namespace?.takeUnless { it.isBlank() || it == "null" }, itemName, context)
    }

    @DELETE
    @Path("resources/{resourceName}/{namespace}/{itemName}") // TODO: why doesn't "resources/{namespace}/{itemName}" work?
    fun deleteResourceItem(@RestPath("resourceName") resourceName: String, @RestPath("namespace") namespace: String?, @RestPath("itemName") itemName: String, @RestQuery("context") context: String? = null) {
        service.deleteResourceItem(resourceName, namespace?.takeUnless { it.isBlank() || it == "null" }, itemName, context)
    }

    @PATCH
    @Path("resources/{resourceName}/{namespace}/{itemName}")
    fun patchResourceItem(@RestPath("resourceName") resourceName: String, @RestPath("namespace") namespace: String?, @RestPath("itemName") itemName: String, @RestQuery("context") context: String? = null, @RestQuery("scaleTo") scaleTo: Int? = null) {
        service.patchResourceItem(resourceName, namespace?.takeUnless { it.isBlank() || it == "null" }, itemName, context, scaleTo)
    }

}