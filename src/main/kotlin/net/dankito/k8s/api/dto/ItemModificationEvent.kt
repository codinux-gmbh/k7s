package net.dankito.k8s.api.dto

data class ItemModificationEvent(
    val itemId: String,
    val html: String? = null
)