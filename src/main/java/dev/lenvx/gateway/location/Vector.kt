package dev.lenvx.gateway.location

import com.google.common.base.Preconditions
import com.google.common.primitives.Doubles
import dev.lenvx.gateway.utils.NumberConversions
import dev.lenvx.gateway.world.World
import java.util.*
import kotlin.math.*

class Vector : Cloneable {

    var x: Double
    var y: Double
    var z: Double

    constructor() {
        this.x = 0.0
        this.y = 0.0
        this.z = 0.0
    }

    constructor(x: Int, y: Int, z: Int) {
        this.x = x.toDouble()
        this.y = y.toDouble()
        this.z = z.toDouble()
    }

    constructor(x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(x: Float, y: Float, z: Float) {
        this.x = x.toDouble()
        this.y = y.toDouble()
        this.z = z.toDouble()
    }

    fun add(vec: Vector): Vector {
        x += vec.x
        y += vec.y
        z += vec.z
        return this
    }

    fun subtract(vec: Vector): Vector {
        x -= vec.x
        y -= vec.y
        z -= vec.z
        return this
    }

    fun multiply(vec: Vector): Vector {
        x *= vec.x
        y *= vec.y
        z *= vec.z
        return this
    }

    fun divide(vec: Vector): Vector {
        x /= vec.x
        y /= vec.y
        z /= vec.z
        return this
    }

    fun copy(vec: Vector): Vector {
        x = vec.x
        y = vec.y
        z = vec.z
        return this
    }

    fun length(): Double {
        return sqrt(NumberConversions.square(x) + NumberConversions.square(y) + NumberConversions.square(z))
    }

    fun lengthSquared(): Double {
        return NumberConversions.square(x) + NumberConversions.square(y) + NumberConversions.square(z)
    }

    fun distance(o: Vector): Double {
        return sqrt(NumberConversions.square(x - o.x) + NumberConversions.square(y - o.y) + NumberConversions.square(z - o.z))
    }

    fun distanceSquared(o: Vector): Double {
        return NumberConversions.square(x - o.x) + NumberConversions.square(y - o.y) + NumberConversions.square(z - o.z)
    }

    fun angle(other: Vector): Float {
        val dot = Doubles.constrainToRange(dot(other) / (length() * other.length()), -1.0, 1.0)
        return acos(dot).toFloat()
    }

    fun midpoint(other: Vector): Vector {
        x = (x + other.x) / 2
        y = (y + other.y) / 2
        z = (z + other.z) / 2
        return this
    }

    fun getMidpoint(other: Vector): Vector {
        val x = (this.x + other.x) / 2
        val y = (this.y + other.y) / 2
        val z = (this.z + other.z) / 2
        return Vector(x, y, z)
    }

    fun multiply(m: Int): Vector {
        x *= m.toDouble()
        y *= m.toDouble()
        z *= m.toDouble()
        return this
    }

    fun multiply(m: Double): Vector {
        x *= m
        y *= m
        z *= m
        return this
    }

    fun multiply(m: Float): Vector {
        x *= m.toDouble()
        y *= m.toDouble()
        z *= m.toDouble()
        return this
    }

    fun dot(other: Vector): Double {
        return x * other.x + y * other.y + z * other.z
    }

    fun crossProduct(o: Vector): Vector {
        val newX = y * o.z - o.y * z
        val newY = z * o.x - o.z * x
        val newZ = x * o.y - o.x * y

        x = newX
        y = newY
        z = newZ
        return this
    }

    fun getCrossProduct(o: Vector): Vector {
        val x = this.y * o.z - o.y * this.z
        val y = this.z * o.x - o.z * this.x
        val z = this.x * o.y - o.x * this.y
        return Vector(x, y, z)
    }

    fun normalize(): Vector {
        val length = length()
        x /= length
        y /= length
        z /= length
        return this
    }

    fun zero(): Vector {
        x = 0.0
        y = 0.0
        z = 0.0
        return this
    }

    fun normalizeZeros(): Vector {
        if (x == -0.0) x = 0.0
        if (y == -0.0) y = 0.0
        if (z == -0.0) z = 0.0
        return this
    }

    fun isInAABB(min: Vector, max: Vector): Boolean {
        return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z
    }

    fun isInSphere(origin: Vector, radius: Double): Boolean {
        return (NumberConversions.square(origin.x - x) + NumberConversions.square(origin.y - y) + NumberConversions.square(origin.z - z)) <= NumberConversions.square(radius)
    }

    fun isNormalized(): Boolean {
        return abs(this.lengthSquared() - 1) < 0.000001
    }

    fun rotateAroundX(angle: Double): Vector {
        val angleCos = cos(angle)
        val angleSin = sin(angle)

        val y = angleCos * this.y - angleSin * z
        val z = angleSin * this.y + angleCos * z
        this.y = y
        this.z = z
        return this
    }

    fun rotateAroundY(angle: Double): Vector {
        val angleCos = cos(angle)
        val angleSin = sin(angle)

        val x = angleCos * this.x + angleSin * z
        val z = -angleSin * this.x + angleCos * z
        this.x = x
        this.z = z
        return this
    }

    fun rotateAroundZ(angle: Double): Vector {
        val angleCos = cos(angle)
        val angleSin = sin(angle)

        val x = angleCos * this.x - angleSin * this.y
        val y = angleSin * this.x + angleCos * this.y
        this.x = x
        this.y = y
        return this
    }

    @Throws(IllegalArgumentException::class)
    fun rotateAroundAxis(axis: Vector, angle: Double): Vector {
        Preconditions.checkArgument(axis != null, "The provided axis vector was null")
        return rotateAroundNonUnitAxis(if (axis.isNormalized()) axis else axis.clone().normalize(), angle)
    }

    @Throws(IllegalArgumentException::class)
    fun rotateAroundNonUnitAxis(axis: Vector, angle: Double): Vector {
        Preconditions.checkArgument(axis != null, "The provided axis vector was null")

        val x = this.x
        val y = this.y
        val z = this.z
        val x2 = axis.x
        val y2 = axis.y
        val z2 = axis.z

        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        val dotProduct = this.dot(axis)

        val xPrime = x2 * dotProduct * (1.0 - cosTheta) +
                x * cosTheta +
                (-z2 * y + y2 * z) * sinTheta
        val yPrime = y2 * dotProduct * (1.0 - cosTheta) +
                y * cosTheta +
                (z2 * x - x2 * z) * sinTheta
        val zPrime = z2 * dotProduct * (1.0 - cosTheta) +
                z * cosTheta +
                (-y2 * x + x2 * y) * sinTheta

        this.x = xPrime
        this.y = yPrime
        this.z = zPrime
        return this
    }

    val blockX: Int
        get() = NumberConversions.floor(x)

    val blockY: Int
        get() = NumberConversions.floor(y)

    val blockZ: Int
        get() = NumberConversions.floor(z)

    override fun equals(other: Any?): Boolean {
        if (other !is Vector) {
            return false
        }
        return abs(x - other.x) < epsilon && abs(y - other.y) < epsilon && abs(z - other.z) < epsilon && (this.javaClass == other.javaClass)
    }

    override fun hashCode(): Int {
        var hash = 7
        hash = 79 * hash + (x.toRawBits() xor (x.toRawBits() ushr 32)).toInt()
        hash = 79 * hash + (y.toRawBits() xor (y.toRawBits() ushr 32)).toInt()
        hash = 79 * hash + (z.toRawBits() xor (z.toRawBits() ushr 32)).toInt()
        return hash
    }

    public override fun clone(): Vector {
        return super.clone() as Vector
    }

    override fun toString(): String {
        return "$x,$y,$z"
    }

    fun toLocation(world: World): Location {
        return Location(world, x, y, z)
    }

    fun toLocation(world: World, yaw: Float, pitch: Float): Location {
        return Location(world, x, y, z, yaw, pitch)
    }

    @Throws(IllegalArgumentException::class)
    fun checkFinite() {
        NumberConversions.checkFinite(x, "x not finite")
        NumberConversions.checkFinite(y, "y not finite")
        NumberConversions.checkFinite(z, "z not finite")
    }

    companion object {
        private val random = Random()
        const val epsilon = 0.000001

        @JvmStatic
        fun getMinimum(v1: Vector, v2: Vector): Vector {
            return Vector(min(v1.x, v2.x), min(v1.y, v2.y), min(v1.z, v2.z))
        }

        @JvmStatic
        fun getMaximum(v1: Vector, v2: Vector): Vector {
            return Vector(max(v1.x, v2.x), max(v1.y, v2.y), max(v1.z, v2.z))
        }

        @JvmStatic
        fun getRandom(): Vector {
            return Vector(random.nextDouble(), random.nextDouble(), random.nextDouble())
        }
    }
}


