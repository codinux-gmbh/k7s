package net.dankito.k8s.domain.model.stats

import java.time.Instant

data class RlimitStats(
    val time: Instant,
    /**
     * The number of running process (threads, precisely on Linux) in the OS.
     */
    val curproc: ULong,
    /**
     * The max number of extant process (threads, precisely on Linux) of OS. See RLIMIT_NPROC in getrlimit(2).
     * The operating system ceiling on the number of process IDs that can be assigned.
     * On Linux, tasks (either processes or threads) consume 1 PID each.
     */
    val maxpid: ULong
)