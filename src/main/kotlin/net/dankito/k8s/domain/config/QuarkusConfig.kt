package net.dankito.k8s.domain.config

import io.quarkus.runtime.annotations.RegisterForReflection
import net.dankito.k8s.api.dto.HomePageData
import net.dankito.k8s.api.dto.ResourceItemsViewData
import net.dankito.k8s.domain.model.KubernetesResource
import net.dankito.k8s.domain.model.ResourceItem
import net.dankito.k8s.domain.model.ResourceItems

@RegisterForReflection(
    targets = [
        // Quarkus won't find them in native mode otherwise
        HomePageData::class, ResourceItemsViewData::class,
        KubernetesResource::class, ResourceItem::class, ResourceItems::class
    ]
)
class QuarkusConfig