package dev.lenvx.gateway.location

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.utils.NumberConversions
import dev.lenvx.gateway.world.BlockState
import dev.lenvx.gateway.world.World
import kotlin.math.*

class Location(
    var world: World,
    var x: Double,
    var y: Double,
    var z: Double,
    var yaw: Float = 0f,
    var pitch: Float = 0f
) : Cloneable {

    constructor(world: World, x: Double, y: Double, z: Double) : this(world, x, y, z, 0f, 0f)

    public override fun clone(): Location {
        return super.clone() as Location
    }

    var blockState: BlockState
        get() = world.getBlock(x.toInt(), y.toInt(), z.toInt())
        set(state) {
            world.setBlock(x.toInt(), y.toInt(), z.toInt(), state)
        }

    val isWorldLoaded: Boolean
        get() = Gateway.instance!!.getWorld(world.name) != null

    
    fun getDirection(): Vector {
        val vector = Vector()

        val rotX = yaw.toDouble()
        val rotY = pitch.toDouble()

        vector.y = -sin(Math.toRadians(rotY))

        val xz = cos(Math.toRadians(rotY))

        vector.x = -xz * sin(Math.toRadians(rotX))
        vector.z = xz * cos(Math.toRadians(rotX))

        return vector
    }

    
    fun setDirection(vector: Vector): Location {
        val _2PI = 2 * PI
        val x = vector.x
        val z = vector.z

        if (x == 0.0 && z == 0.0) {
            pitch = if (vector.y > 0) -90f else 90f
            return this
        }

        val theta = atan2(-x, z)
        yaw = Math.toDegrees((theta + _2PI) % _2PI).toFloat()

        val x2 = NumberConversions.square(x)
        val z2 = NumberConversions.square(z)
        val xz = sqrt(x2 + z2)
        pitch = Math.toDegrees(atan(-vector.y / xz)).toFloat()

        return this
    }

    fun add(vec: Location): Location {
        require(!(vec.world != world)) { "Cannot add Locations of differing worlds" }

        x += vec.x
        y += vec.y
        z += vec.z
        return this
    }

    fun add(vec: Vector): Location {
        this.x += vec.x
        this.y += vec.y
        this.z += vec.z
        return this
    }

    fun add(x: Double, y: Double, z: Double): Location {
        this.x += x
        this.y += y
        this.z += z
        return this
    }

    fun subtract(vec: Location): Location {
        require(!(vec.world != world)) { "Cannot add Locations of differing worlds" }

        x -= vec.x
        y -= vec.y
        z -= vec.z
        return this
    }

    fun subtract(vec: Vector): Location {
        this.x -= vec.x
        this.y -= vec.y
        this.z -= vec.z
        return this
    }

    fun subtract(x: Double, y: Double, z: Double): Location {
        this.x -= x
        this.y -= y
        this.z -= z
        return this
    }

    fun length(): Double {
        return sqrt(NumberConversions.square(x) + NumberConversions.square(y) + NumberConversions.square(z))
    }

    fun lengthSquared(): Double {
        return NumberConversions.square(x) + NumberConversions.square(y) + NumberConversions.square(z)
    }

    fun distance(o: Location): Double {
        return sqrt(distanceSquared(o))
    }

    fun distanceSquared(o: Location?): Double {
        requireNotNull(o) { "Cannot measure distance to a null location" }
        require(!(o.world == null || world == null)) { "Cannot measure distance to a null world" }
        require(!(o.world != world)) { "Cannot measure distance between ${world.name} and ${o.world.name}" }

        return NumberConversions.square(x - o.x) + NumberConversions.square(y - o.y) + NumberConversions.square(z - o.z)
    }

    fun multiply(m: Double): Location {
        x *= m
        y *= m
        z *= m
        return this
    }

    fun zero(): Location {
        x = 0.0
        y = 0.0
        z = 0.0
        return this
    }

    fun toVector(): Vector {
        return Vector(x, y, z)
    }

    fun checkFinite() {
        NumberConversions.checkFinite(x, "x not finite")
        NumberConversions.checkFinite(y, "y not finite")
        NumberConversions.checkFinite(z, "z not finite")
        NumberConversions.checkFinite(pitch.toDouble(), "pitch not finite")
        NumberConversions.checkFinite(yaw.toDouble(), "yaw not finite")
    }

    override fun toString(): String {
        return "Location{world=$world,x=$x,y=$y,z=$z,pitch=$pitch,yaw=$yaw}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Location) return false

        if (world != other.world) return false
        if (x.toRawBits() != other.x.toRawBits()) return false
        if (y.toRawBits() != other.y.toRawBits()) return false
        if (z.toRawBits() != other.z.toRawBits()) return false
        if (pitch.toRawBits() != other.pitch.toRawBits()) return false
        if (yaw.toRawBits() != other.yaw.toRawBits()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = world.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        result = 31 * result + pitch.hashCode()
        result = 31 * result + yaw.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        fun locToBlock(loc: Double): Int {
            return NumberConversions.floor(loc)
        }

        @JvmStatic
        fun normalizeYaw(yaw: Float): Float {
            var y = yaw % 360.0f
            if (y >= 180.0f) {
                y -= 360.0f
            } else if (y < -180.0f) {
                y += 360.0f
            }
            return y
        }

        @JvmStatic
        fun normalizePitch(pitch: Float): Float {
            return if (pitch > 90.0f) {
                90.0f
            } else if (pitch < -90.0f) {
                -90.0f
            } else {
                pitch
            }
        }
    }
}


