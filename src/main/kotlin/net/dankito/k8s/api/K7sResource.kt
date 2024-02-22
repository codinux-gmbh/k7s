package net.dankito.k8s.api

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.dankito.k8s.domain.service.KubernetesService

@Path("/resources")
@Produces(MediaType.APPLICATION_JSON)
class K7sResource(
    private val service: KubernetesService
) {

    @GET
    fun getAllAvailableResourceTypes() =
        service.getAllAvailableResourceTypes()

}