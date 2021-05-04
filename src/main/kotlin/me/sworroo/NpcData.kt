package me.sworroo

import dev.xdark.clientapi.entity.Entity
import dev.xdark.clientapi.entity.EntityLivingBase

data class NpcData(
    val entityId: Int? = null,
    var entity : EntityLivingBase? = null,
    val worldName: String? = null,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val displayName: String? = null,
    val skinUrl: String? = null,
    val skinDigest: String? = null,
    val skinType: String? = null,
    val headRotation : Boolean = false
)
