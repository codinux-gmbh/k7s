package net.dankito.k8s.domain.config

import io.quarkus.runtime.annotations.RegisterForReflection
import net.dankito.k8s.api.dto.HomePageData
import net.dankito.k8s.domain.model.KubernetesResource

@RegisterForReflection(
    targets = [
        // Quarkus won't find them in native mode otherwise
        HomePageData::class,
        KubernetesResource::class
    ]
)
class QuarkusConfig