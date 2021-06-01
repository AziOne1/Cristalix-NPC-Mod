package me.sworroo

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import dev.xdark.clientapi.ClientApi
import dev.xdark.clientapi.entity.*
import dev.xdark.clientapi.entry.ModMain
import dev.xdark.clientapi.event.chunk.ChunkLoad
import dev.xdark.clientapi.event.chunk.ChunkUnload
import dev.xdark.clientapi.event.input.KeyPress
import dev.xdark.clientapi.event.lifecycle.GameLoop
import dev.xdark.clientapi.event.network.PluginMessage
import dev.xdark.clientapi.world.chunk.Chunk
import dev.xdark.feder.NetUtil
import org.lwjgl.input.Keyboard
import ru.cristalix.uiengine.UIEngine
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.atan2
import kotlin.math.sqrt

class YouTube : ModMain {

    private var npcs1: ArrayList<NpcData> = arrayListOf()

    override fun load(api: ClientApi) {
        UIEngine.initialize(api)

        fun createNpc(npcData: NpcData) : EntityLivingBase {
            val npc = api.entityProvider().newEntity(1000, api.minecraft().world)
            //var npc = npc1 as EntityLivingBase
            if(npcData.entityId != null){
                npc.entityId = npcData.entityId
            }
            val id = UUID.randomUUID()
            npc as AbstractClientPlayer
            val profile = GameProfile(id, npcData.displayName)
            if(npcData.skinUrl != null && npcData.skinDigest != null){
                profile.properties.put("skinURL", Property("skinURL", npcData.skinUrl, ""))
                profile.properties.put("skinDigest", Property("skinDigest", npcData.skinDigest, ""))
            }
            npc.gameProfile = profile

            val info = api.clientConnection().newPlayerInfo(profile)
            npc.setUniqueId(profile.id)
            npc.setWearing(PlayerModelPart.CAPE)
            npc.setWearing(PlayerModelPart.HAT)
            npc.setWearing(PlayerModelPart.JACKET)
            npc.setWearing(PlayerModelPart.LEFT_PANTS_LEG)
            npc.setWearing(PlayerModelPart.LEFT_SLEEVE)
            npc.setWearing(PlayerModelPart.RIGHT_PANTS_LEG)
            npc.setWearing(PlayerModelPart.RIGHT_SLEEVE)

            info.responseTime = -2
            if(npcData.skinType != null){
                info.skinType = npcData.skinType
            }
            api.clientConnection().addPlayerInfo(info)

            return npc
        }

        fun die(entity: Entity){
            api.minecraft().world.removeEntity(entity)
        }

        fun square(value: Double): Double {
            return value*value
        }

        val PI = 3.14159265358979323846;

        fun toDegress(value: Double): Double {
            return value * 180.0/ PI
        }

        fun isInsideOfChunk(chunk: Chunk, x: Double, z: Double): Boolean {
            return ((x.toInt() shr 4) == chunk.x) && ((z.toInt() shr 4) == chunk.z)
        }
        var currentWorld = "world";
        UIEngine.registerHandler(ChunkLoad::class.java, 1) {
            var i = 0
            while (i < npcs1.size) {
                var npc = npcs1[i]
                if (npc.worldName == currentWorld) {
                    if (isInsideOfChunk(this.chunk, npc.x, npc.z)) {
                        var npcEntity = createNpc(npc)
                        api.minecraft().world.spawnEntity(npcEntity)
                        npcEntity.teleport(npc.x, npc.y, npc.z)
                        npc.entity = npcEntity
                    }
                }
                i++
            }
        }

        UIEngine.registerHandler(ChunkUnload::class.java, 1){
            var i = 0
            while(i<npcs1.size){
                var npc = npcs1[i]
                if(npc.worldName == currentWorld){
                    if(isInsideOfChunk(this.chunk, npc.x, npc.z)){
                        npc.entity = null
                    }
                }
                i++
            }
        }

        var period : Long = 0

        UIEngine.registerHandler(PluginMessage::class.java, 1){
            if(this.channel == "ilyafx_npcs"){
                var state = NetUtil.readUtf8(this.data)
                if(state == "change_world"){
                    currentWorld = NetUtil.readUtf8(this.data)
                    var i = 0
                    while (i<npcs1.size){
                        val npc = npcs1[i]
                        if(npc.entity != null){
                            die(npc.entity!!)
                        }
                        i++
                    }
                    val minecraftWorld = api.minecraft().world
                    i = 0
                    while (i < npcs1.size){
                        val npc = npcs1[i]
                        if(npc.worldName == currentWorld){
                            val chunk = api.minecraft().world.chunkProvider.getLoadedChunk(npc.x.toInt() shr 4, npc.z.toInt() shr 4)
                            if(chunk != null){
                                var npcEntity = createNpc(npc)
                                api.minecraft().world.spawnEntity(npcEntity)
                                npcEntity.teleport(npc.x, npc.y, npc.z)
                                npc.entity = npcEntity
                            }
                        }
                        i++
                    }
                }else if ("set_npcs" == state){
                    var count = NetUtil.readVarInt(this.data)
                    var newNpcs : ArrayList<NpcData> = arrayListOf()
                    var i = 0
                    while (i< count){
                        var entityId = NetUtil.readVarInt(this.data)
                        var world = NetUtil.readUtf8(this.data)
                        var x = this.data.readDouble()
                        var y = this.data.readDouble()
                        var z = this.data.readDouble()
                        var name = NetUtil.readUtf8(this.data)
                        var hasSkin = this.data.readBoolean()
                        var skin : String? = null
                        var digest : String? = null
                        var skinType : String? = null
                        if(hasSkin){
                            skin = NetUtil.readUtf8(this.data)
                            digest = NetUtil.readUtf8(this.data)
                            skinType = NetUtil.readUtf8(this.data)
                        }
                        var headRotation = this.data.readBoolean()
                        newNpcs.add(NpcData(
                            entityId, null, world, x,y,z,name,skin,digest,skinType,headRotation
                        ))
                        var t = 0
                        while(t<npcs1.size){
                            val npc = npcs1[t]
                            if(npc.entity != null){
                                die(npc.entity!!)
                            }
                            t++
                        }
                        npcs1 = newNpcs
                        t = 0
                        while (t < npcs1.size){
                            val npc = npcs1[t]
                            if(npc.worldName == currentWorld){
                                val chunk = api.minecraft().world.chunkProvider.getLoadedChunk(npc.x.toInt() shr 4, npc.z.toInt() shr 4)
                                if(chunk != null){
                                    var npcEntity = createNpc(npc)
                                    api.minecraft().world.spawnEntity(npcEntity)
                                    npcEntity.teleport(npc.x, npc.y, npc.z)
                                    npc.entity = npcEntity
                                }
                            }
                            t++
                        }
                        i++
                    }
                }
            }

        }

        UIEngine.registerHandler(GameLoop::class.java, 1){
            val now = System.currentTimeMillis()
            if(api.minecraft().player != null){
                val playerX = api.minecraft().player.x
                val playerY = api.minecraft().player.y
                val playerZ = api.minecraft().player.z
                if(now - period >= 50){
                    period = now
                    var i = 0
                    while (i<npcs1.size){
                        val npc = npcs1[i]
                        if(npc.headRotation && (npc.entity != null)){
                            val dx: Double = api.minecraft().player.x - npc.x
                            var dy: Double = api.minecraft().player.y - npc.y
                            val dz: Double = api.minecraft().player.z - npc.z
                            dy /= Math.sqrt(dx*dx+dz*dz);
                            val yawDeg : Double = (Math.atan2(-dx, dz) / Math.PI * 180)
                            val pitchDeg = toDegress(atan2(npc.y - playerY, sqrt(square(npc.x - playerX)+square(npc.z - playerZ) )))
                            npc.entity!!.setYaw(yawDeg.toFloat())
                            npc.entity!!.rotationYawHead = yawDeg.toFloat()
                            npc.entity!!.setPitch(pitchDeg.toFloat())
                        }
                        i++
                    }
                }

            }
        }


    }

    override fun unload() {
        UIEngine.uninitialize()
    }
}