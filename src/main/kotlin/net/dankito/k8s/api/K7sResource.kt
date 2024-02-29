package net.dankito.k8s.api

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import net.dankito.k8s.domain.service.KubernetesService
import org.jboss.resteasy.reactive.RestPath

@Path("")
@Produces(MediaType.APPLICATION_JSON)
class K7sResource(
    private val service: KubernetesService
) {

    @Path("resources")
    @GET // TODO: why doesn't KubernetesClient work with suspending / non-blocking function?
    fun getAllAvailableResourceTypes() =
        service.getAllAvailableResourceTypes()

    @Path("resource/{resourceName}/{namespace}/{itemName}") // TODO: why doesn't "resources/{namespace}/{itemName}" work?
    @DELETE
    fun deleteResourceItem(@RestPath("resourceName") resourceName: String, @RestPath("namespace") namespace: String?, @RestPath("itemName") itemName: String) {
        service.deleteResourceItem(resourceName, namespace?.takeUnless { it.isBlank() || it == "null" }, itemName)
    }

}