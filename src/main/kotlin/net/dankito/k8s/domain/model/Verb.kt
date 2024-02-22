package net.dankito.k8s.domain.model

enum class Verb {
    create,
    get,
    list,
    watch,
    update,
    patch,
    delete,
    deletecollection;

    companion object {
        private val byName = Verb.entries.map { it.name to it }.toMap()

        fun getByName(verb: String): Verb? = byName[verb.lowercase()]
    }

}