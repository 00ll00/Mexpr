package oolloo.mexper

import kotlin.math.abs
import kotlin.math.max

/**
 * # 梗体表达式池
 *
 * 为给定的连续范围内的**整数**创建至少一个梗体表达式，并使表达式尽可能的简短。**不会检查对于提供的基础元素和运算能否完成生成**，可能会进入死循环。
 *
 * 使用 `MexprPool.get(int)` 方法抽取生成的表达式字符串。
 *
 * @param initialItems 梗体表达式中的基础元素
 * @param operations 梗体表达式中允许的运算
 * @param genRange 生成整数的范围
 * @param cacheRange_ 生成过程中允许暂存的整数范围，即中间表达式的范围，默认为所需范围中绝对值最大者的两倍
 * @param maxCacheNum 允许为一个整数生成的表达式最大数量，值越高生成的表达式越多样化，默认为5
 * @param quality 生成表达式的简化程度，取值范围为 [[0, 1]]，默认为1。需要提供的运算部分或全部实现了反向运算方法时生效，通常取值0.75左右时能够获得最高效率
 */
class MexprPool(initialItems: List<Mexpr>, operations: List<Op>, val genRange: IntRange, cacheRange_: IntRange? = null, maxCacheNum: Int = 5, quality: Float = 1f) {

    private val cacheRange: IntRange
    private val values = HashMap<Int, ArrayList<Mexpr>>()

    init {
        // check  arguments
        cacheRange = if (cacheRange_ == null) {
            val m = max(abs(genRange.first), abs(genRange.last)) * 2
            -m..m
        } else {
            if (cacheRange_.first > genRange.first || cacheRange_.last < genRange.last) throw IllegalArgumentException("cacheRange should cover genRange.")
            cacheRange_
        }
        if (maxCacheNum < 1) throw IllegalArgumentException("cache num should greater than 0.")
        if (initialItems.isEmpty()) throw IllegalArgumentException("empty initial items.")
        if (operations.isEmpty()) throw IllegalArgumentException("empty operations.")
        if (quality !in 0f..1f) throw IllegalArgumentException("quality should in range [0, 1].")

        // generate
        val cache = HashMap<Int, ArrayList<Mexpr>>()
        for (i in initialItems) {
            if (i.value in cache) {
                cache[i.value]!!.add(i)
            } else {
                cache[i.value] = arrayListOf(i)
            }
        }
        var forward = true
        var m = 1
        while (genRange.any { it !in cache }) {
            if (forward) {  // forward generating to create most of the values
                m += 1
                for (i in cacheRange) if (i in cache) for (j in cacheRange) if (j in cache) for (op in operations) if (op.canForward(i, j)) {
                    val v = op.forward(i, j)
                    if (v in cacheRange) {
                        for (k in 0 until maxCacheNum) {
                            val item1 = cache[i]!!.randomOrNull() ?: break
                            val item2 = cache[j]!!.randomOrNull() ?: break
                            val mass = op.calcMass(item1, item2)
                            if (mass >= m || v in cache && mass > cache[v]!![0].mass)  continue
                            if (v !in cache) cache[v] = ArrayList()
                            val resArray = cache[v]!!
                            resArray.add(Mexpr(v, op.buildText(item1, item2), mass, op))
                            if (resArray.size > maxCacheNum) resArray.remove(resArray.random())  // drop random item while achieve max cache
                        }
                    }
                }
                forward = genRange.sumOf{ if (it in cache) 1 as Int else 0 } < (genRange.last - genRange.first) * quality
            }
            else {  // backward generating to accelerate the process
                for (v in genRange) if (v !in cache) for (i in cacheRange) if (i in cache) for (op in operations) {
                    if (op.canBackwardLeft(v, i)) {
                        val j = op.backwardLeft(v, i)
                        if (j in cache) {
                            cache[v] = arrayListOf()
                            val resArray = cache[v]!!
                            for (k in 0 until maxCacheNum) {
                                val item1 = cache[i]!!.randomOrNull() ?: break
                                val item2 = cache[j]!!.randomOrNull() ?: break
                                val mass = op.calcMass(item1, item2)
                                if (resArray.size > 0) {
                                    if (mass > resArray[0].mass) {
                                        continue
                                    } else if (mass < resArray[0].mass) {
                                        resArray.clear()
                                    }
                                }
                                resArray.add(Mexpr(v, op.buildText(item1, item2), item1.mass + item2.mass, op))
                                if (resArray.size > maxCacheNum) resArray.remove(resArray.random())  // drop random item while achieve max cache
                            }
                        }
                    }
                    if (op.canBackwardRight(v, i)) {
                        val j = op.backwardRight(v, i)
                        if (j in cache) {
                            cache[v] = arrayListOf()
                            val resArray = cache[v]!!
                            for (item1 in cache[i]!!.clone() as List<Mexpr>) for (item2 in cache[j]!!.clone() as List<Mexpr>) {
                                val mass = op.calcMass(item1, item2)
                                if (resArray.size > 0) {
                                    if (mass > resArray[0].mass) {
                                        continue
                                    } else if (mass < resArray[0].mass) {
                                        resArray.clear()
                                    }
                                }
                                resArray.add(Mexpr(v, op.buildText(item1, item2), item1.mass + item2.mass, op))
                                if (resArray.size > maxCacheNum) resArray.remove(resArray.random())  // drop random item while achieve max cache
                            }
                        }
                    }
                }
            }
        }
        for (i in genRange) {
            values[i] = cache[i]!!
        }
    }

    fun get(int: Int): String {
        if (int !in genRange) throw IllegalArgumentException("given integer not in gen range.")
        return values[int]!!.random().text
    }

    fun getMexpr(int: Int): Mexpr {
        if (int !in genRange) throw IllegalArgumentException("given integer not in gen range.")
        return values[int]!!.random()
    }

    fun getAllMexpr(int: Int): List<Mexpr> {
        if (int !in genRange) throw IllegalArgumentException("given integer not in gen range.")
        return values[int]!!
    }
}

/**
 * # 梗体表达式
 *
 * @property value 表达式对应的值
 * @property text 表达式的文本
 * @property mass 表达式的重量（复杂度），默认基本元素取1
 * @property op 表达式最外层的操作，基本元素应设置为`Op.PRIMITIVE`
 */
data class Mexpr(
    val value: Int,
    val text: String,
    val mass: Int = 1,
    val op: Op = Op.PRIMITIVE
) {
    fun wrap(level: Int): String = if (op.level >= level) text else "($text)"
}

/**
 * # 表达式运算操作
 *
 * 定义运算的逻辑和表达式文本的生成过程。
 *
 * 若实现了逆推方法则当降低`MexprPool`的`quality`时可以提升生成速度。
 *
 * 只支持二元运算操作。
 *
 * @property level 操作的等级，用于默认运算符生成文本时进行是否需要加括号的判断，与运算符优先级有一定区别，可以不使用
 */
interface Op {
    val level: Int
    /**
     * 检查提供的参数能否进行运算
     */
    fun canForward(left: Int, right: Int): Boolean

    /**
     * 计算运算结果
     */
    fun forward(left: Int, right: Int): Int

    /**
     * 根据提供的表达式参数生成运算后的表达式文本
     */
    fun buildText(left: Mexpr, right: Mexpr): String

    /**
     * 计算生成的表达式的重量，默认为两个子表达式重量之和
     */
    fun calcMass(left: Mexpr, right: Mexpr): Int = left.mass + right.mass

    /**
     * 检查提供的参数能否进行逆推右参数，用于加速生成。若启用此方法则必须实现`backwardLeft`。
     */
    fun canBackwardLeft(res: Int, left: Int): Boolean = false

    /**
     * 根据提供的结果和左参数逆推右参数，用于加速生成。若启用此方法则必须实现`canBackwardLeft`。
     */
    fun backwardLeft(res: Int, left: Int): Int = 0

    /**
     * 检查提供的参数能否进行逆推左参数，用于加速生成。若启用此方法则必须实现`backwardRight`。
     */
    fun canBackwardRight(res: Int, right: Int): Boolean = false

    /**
     * 根据提供的结果和右参数逆推左参数，用于加速生成。若启用此方法则必须实现`canBackwardRight`。
     */
    fun backwardRight(res: Int, right: Int): Int = 0

    /*
    level:
    0 -> |
    1 -> ^
    2 -> &
    3 -> +, -
    4 -> %
    5 -> *, //
    inf -> primitive
     */

    /**
     * ## 初始原始使用的运算操作
     */
    object PRIMITIVE: Op {
        override val level = Int.MAX_VALUE
        override fun canForward(left: Int, right: Int): Boolean = false
        override fun forward(left: Int, right: Int): Int = 0
        override fun buildText(left: Mexpr, right: Mexpr): String = ""
        override fun canBackwardLeft(res: Int, left: Int): Boolean = false
        override fun backwardLeft(res: Int, left: Int): Int = 0
        override fun canBackwardRight(res: Int, right: Int): Boolean = false
        override fun backwardRight(res: Int, right: Int): Int = 0
    }

    /**
     * ## 默认的加法运算实现
     */
    open class Add: Op {
        open val text: String = " + "
        override val level = 3
        override fun canForward(left: Int, right: Int) = true
        override fun forward(left: Int, right: Int) = left + right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(3) + text + right.wrap(3)
        override fun canBackwardLeft(res: Int, left: Int): Boolean = true
        override fun backwardLeft(res: Int, left: Int): Int = res - left
    }

    /**
     * ## 默认的减法运算实现
     */
    open class  Sub: Op {
        open val text: String = " - "
        override val level = 3
        override fun canForward(left: Int, right: Int) = true
        override fun forward(left: Int, right: Int) = left - right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(3) + text + right.wrap(4)
        override fun canBackwardLeft(res: Int, left: Int) = true
        override fun backwardLeft(res: Int, left: Int): Int = res + left
        override fun canBackwardRight(res: Int, right: Int): Boolean = true
        override fun backwardRight(res: Int, right: Int): Int = right - res
    }

    /**
     * ## 默认的乘法运算实现
     */
    open class  Mul: Op {
        open val text: String = " * "
        override val level = 5
        override fun canForward(left: Int, right: Int) = true
        override fun forward(left: Int, right: Int) = left * right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(5) + text + right.wrap(5)
        override fun canBackwardLeft(res: Int, left: Int): Boolean = left != 0 && res % left == 0
        override fun backwardLeft(res: Int, left: Int): Int = res / left
    }

    /**
     * ## 默认的除法运算实现
     *
     * 会避免出现除数为0和非整除的情况出现
     */
    open class  Div: Op {
        open val text: String = " / "
        override val level = 5
        override fun canForward(left: Int, right: Int) = right != 0 && left % right == 0
        override fun forward(left: Int, right: Int) = left / right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(5) + text + right.wrap(6)
        override fun canBackwardLeft(res: Int, left: Int): Boolean = res != 0 && left % res == 0
        override fun backwardLeft(res: Int, left: Int): Int = left / res
        override fun canBackwardRight(res: Int, right: Int): Boolean = true
        override fun backwardRight(res: Int, right: Int): Int = res * right
    }

    /**
     * ## 默认的模运算实现
     *
     * 会避免出现负数，并保证被除数大于除数
     */
    open class  Mod: Op {
        open val text: String = " % "
        override val level = 4
        override fun canForward(left: Int, right: Int) = right in 1 until left
        override fun forward(left: Int, right: Int) = left % right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(5) + text + right.wrap(6)
    }

    /**
     * ## 默认的位与运算实现
     */
    open class  BitAnd: Op {
        open val text: String = " & "
        override val level = 2
        override fun canForward(left: Int, right: Int) = true
        override fun forward(left: Int, right: Int) = left and right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(2) + text + right.wrap(2)
    }

    /**
     * ## 默认的位或运算实现
     */
    open class  BitOr: Op {
        open val text: String = " | "
        override val level = 0
        override fun canForward(left: Int, right: Int) = true
        override fun forward(left: Int, right: Int) = left or right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(0) + text + right.wrap(0)
    }

    /**
     * ## 默认的位异或运算实现
     */
    open class  BitXor: Op {
        open val text: String = " ^ "
        override val level = 1
        override fun canForward(left: Int, right: Int) = true
        override fun forward(left: Int, right: Int) = left xor right
        override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(1) + text + right.wrap(1)
        override fun canBackwardLeft(res: Int, left: Int) = true
        override fun backwardLeft(res: Int, left: Int) = res xor left
    }
}