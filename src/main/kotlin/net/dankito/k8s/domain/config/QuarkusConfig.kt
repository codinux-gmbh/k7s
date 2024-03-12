package net.dankito.k8s.domain.config

import io.quarkus.runtime.annotations.RegisterForReflection
import net.dankito.k8s.api.dto.HomePageData
import net.dankito.k8s.api.dto.ItemModificationEvent
import net.dankito.k8s.api.dto.ResourceItemRowViewData
import net.dankito.k8s.api.dto.ResourceItemsViewData
import net.dankito.k8s.domain.model.*

@RegisterForReflection(
    targets = [
        // Quarkus won't find them in native mode otherwise
        HomePageData::class, ResourceItemsViewData::class, ResourceItemRowViewData::class,
        ItemModificationEvent::class,
        KubernetesResource::class, ResourceItems::class, ResourceItem::class, ItemValue::class, PodResourceItem::class, ContainerStatus::class
    ]
)
class QuarkusConfig