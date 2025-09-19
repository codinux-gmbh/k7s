package net.dankito.k8s.api

import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import net.dankito.k8s.api.dto.KubeContextResources
import net.dankito.k8s.api.dto.ResourceItemParameter
import net.dankito.k8s.api.dto.ResourceParameter
import net.dankito.k8s.domain.model.ResourceItems
import net.dankito.k8s.domain.service.KubernetesService
import org.jboss.resteasy.reactive.RestPath
import org.jboss.resteasy.reactive.RestQuery

@Path("/api/v1/resources")
@Produces(MediaType.APPLICATION_JSON)
class K7sResource(
    private val service: KubernetesService
) {

    @GET
    fun getContextResources(@RestQuery("context") context: String? = null): KubeContextResources {
        val resources = service.getAllAvailableResourceTypes(context)

        return KubeContextResources(
            resources,
            service.getNamespaces(context)?.items.orEmpty().map { it.name },
            service.contextsNames,
            service.defaultContext.takeUnless { it == KubernetesService.NonNullDefaultContextName },
        )
    }

    @GET
    @Path("/{group}/{kind}")
    fun getResourceItems(@BeanParam params: ResourceParameter): ResourceItems? =
        service.getResourceItems(params)


    @GET
    @Path("/{group}/{kind}/{namespace}/{itemName}/yaml")
    @Produces("application/yaml")
    fun getResourceItemYaml(@BeanParam params: ResourceItemParameter): String? =
        service.getResourceItemYaml(params)

    @PATCH
    @Path("/{group}/{kind}/{namespace}/{itemName}")
    fun scaleResourceItem(@BeanParam params: ResourceItemParameter, @QueryParam("scaleTo") scaleTo: Int): Boolean =
        service.scaleResourceItem(params, scaleTo)

    @DELETE
    @Path("/{group}/{kind}/{namespace}/{itemName}")
    @Produces("application/yaml")
    fun deleteResourceItem(
        @BeanParam params: ResourceItemParameter,
        @QueryParam("gracePeriod") gracePeriodSeconds: Long? = null
    ): Boolean =
        service.deleteResourceItem(params, gracePeriodSeconds)


    // TODO: remove, resourceName may not be unique
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