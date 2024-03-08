package net.dankito.k8s.domain.model.stats

import com.fasterxml.jackson.annotation.JsonProperty

class ProcessStats(
//    @get:JsonProperty("process_count")
//    val processCount: ULong? = null
) {

    // don't know why declaring it has a constructor property causes a deserialization exception with Jackson
    @get:JsonProperty("process_count")
    val processCount: ULong? = null

}