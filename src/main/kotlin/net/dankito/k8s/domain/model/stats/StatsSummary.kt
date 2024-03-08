package net.dankito.k8s.domain.model.stats

/**
 * For definition and documentation of all types see:
 * [https://github.com/kubernetes/kubernetes/blob/master/staging/src/k8s.io/kubelet/pkg/apis/stats/v1alpha1/types.go](https://github.com/kubernetes/kubernetes/blob/master/staging/src/k8s.io/kubelet/pkg/apis/stats/v1alpha1/types.go)
 */
data class StatsSummary(
    val node: NodeStats,
    val pods: List<PodStats>
) {

    companion object {
        // SystemContainerKubelet is the container name for the system container tracking Kubelet usage.
        const val SystemContainerKubelet = "kubelet"
        // SystemContainerRuntime is the container name for the system container tracking the runtime (e.g. docker) usage.
        const val SystemContainerRuntime = "runtime"
        // SystemContainerMisc is the container name for the system container tracking non-kubernetes processes.
        const val SystemContainerMisc = "misc"
        // SystemContainerPods is the container name for the system container tracking user pods.
        const val SystemContainerPods = "pods"
    }

}