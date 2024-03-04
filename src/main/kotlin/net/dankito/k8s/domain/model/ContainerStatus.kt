package net.dankito.k8s.domain.model

class ContainerStatus(
    val name: String,
    val containerID: String?,
    val image: String,
    val imageID: String,
    val restartCount: Int,
    val started: Boolean,
    val ready: Boolean,
    val waiting: Boolean,
    val running: Boolean,
    val terminated: Boolean
    // TODO: state.running, .terminated, .waiting
) {
}