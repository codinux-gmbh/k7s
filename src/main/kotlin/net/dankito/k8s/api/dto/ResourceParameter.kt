package net.dankito.k8s.api.dto

import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter

// JAX-RS @BeanParam fields have to be mutable, therefore var instead of val, otherwise call to method fails.
@RegisterForReflection
class ResourceParameter {
    @field:PathParam("group")
    @field:Parameter(description = "The resource group. May be null for standard resources like Pod.", required = true)
    var group: String? = null

    @field:PathParam("kind")
    @field:Parameter(description = "The Resource kind like Pod", required = true)
    lateinit var kind: String

    @field:QueryParam("context")
    @field:Parameter(description = "The kube context. Only relevant if user has more then one context. If omitted default context is used.", required = false, allowEmptyValue = true)
    var context: String? = null

    @field:QueryParam("namespace")
    @field:Parameter(description = "The namespace of the resource. If omitted all resources are returned.", required = false, allowEmptyValue = true)
    var namespace: String? = null


    fun fixValues(): ResourceParameter = this.also {
        group = fixNull(group)
        context = fixNull(context)
        namespace = fixNull(namespace)
    }

    private fun fixNull(value: String?) =
        value?.takeUnless { it.isBlank() || it == "null" }
}