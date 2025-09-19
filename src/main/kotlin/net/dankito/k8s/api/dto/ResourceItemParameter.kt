package net.dankito.k8s.api.dto

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter

// JAX-RS @BeanParam fields have to be mutable, therefore var instead of val, otherwise call to method fails.
@RegisterForReflection
class ResourceItemParameter {
    @field:PathParam("group")
    @field:Parameter(description = "The resource group. May be null for standard resources like Pod.", required = true, allowEmptyValue = true)
    var group: String? = null

    @field:PathParam("kind")
    @field:Parameter(description = "The Resource kind like Pod", required = true)
    lateinit var kind: String

    @field:PathParam("namespace")
    @field:Parameter(description = "The namespace of the resource. Set to 'null' if it's a cluster resource.", required = false)
    var namespace: String? = null

    @field:PathParam("itemName")
    @field:Parameter(description = "The name of the resource item to fetch like 'grafana-dc874d6fd-xrpd7'", required = true)
    lateinit var itemName: String

    @field:QueryParam("context")
    @field:Parameter(description = "The kube context. Only relevant if user has more then one context. If omitted default context is used.", required = false, allowEmptyValue = true)
    var context: String? = null


    fun fixValues(): ResourceItemParameter = this.also {
        group = fixNull(group)
        namespace = fixNull(namespace)
        context = fixNull(context)
    }

    private fun fixNull(value: String?) =
        value?.takeUnless { it.isBlank() || it == "null" }
}