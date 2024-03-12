package net.dankito.k8s.domain.model

data class ItemValue(
    val name: String,
    val value: String?,
    val mobileValue: String? = null
)
