import oolloo.mexper.Mexpr
import oolloo.mexper.MexprPool
import oolloo.mexper.Op
import oolloo.mexper.Op.*

/**
 * # 在 JavaScript 中不使用数字和字母生成 [[0, 100]] 范围内的所有整数
 *
 * 调整了各运算符的重量计算方法以生成尽可能短的表达式；添加`CatStr`运算以模拟js中 ''+1+1-0 = 11 的效果
 *
 * 为什么会这样：[zhuangbility](https://github.com/walfud/zhuangbility), [wtfjs](https://github.com/denysdovhan/wtfjs)
 */
fun main() {
    val range = 0..100

    // 重写的表达式包装函数以防止连续的加号/减号被解析为自增/自减
    fun Mexpr.wrapSplitPlus(level: Int) = this.wrap(level).let { if (it.startsWith('+')) " $it" else it }
    fun Mexpr.wrapSplitMinus(level: Int) = this.wrap(level).let { if (it.startsWith('-')) " $it" else it }

    // 字符串拼接运算
    val CatStr = object: Op {
        override val level = Int.MAX_VALUE
        override fun canForward(left: Int, right: Int) = right >= 0
        override fun forward(left: Int, right: Int) = "$left$right".toInt()
        // 将连续的字符串连接运算合并到一起
        override fun buildText(left: Mexpr, right: Mexpr): String {
            val leftStr = if (left.op == this) left.text.substring(4..left.text.length - 5) else left.wrapSplitPlus(4)
            val rightStr = if (right.op == this) right.text.substring(4..right.text.length - 5) else right.wrapSplitPlus(4)
            return "([]+${leftStr}+${rightStr}-[])"
        }
        // 改用表达式文本长度衡量重量，使表达式长度尽可能短
        override fun calcMass(left: Mexpr, right: Mexpr) = left.text.length + right.text.length + if (left.op == this || right.op == this) 0 else 8
    }

    val items = arrayListOf(
        Mexpr(-1, "~[]", mass = 3),
        Mexpr(-1, "~{}", mass = 3),
        Mexpr(0, "+[]", mass = 3),
        Mexpr(0, "-[]", mass = 3),
        Mexpr(1, "-~[]", mass = 4),
        Mexpr(1, "-~{}", mass = 4),
    )
    // 改用表达式文本长度衡量重量，使表达式长度尽可能短
    fun calcMass(left: Mexpr, right: Mexpr): Int = left.text.length + right.text.length + 1
    val operations = arrayListOf(
        object: Add() {
            override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(3) + "+" + right.wrapSplitPlus(3)
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                      },
        object: Sub() {
            override fun buildText(left: Mexpr, right: Mexpr) = left.wrap(3) + "-" + right.wrapSplitMinus(4)
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                      },
        object: Mul() {
            override val text = "*"
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                      },
        object: Div() {
            override val text = "/"
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                      },
        object: Mod() {
            override val text = "%"
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                      },
        object: BitAnd() {
            override val text = "&"
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                         },
        object: BitOr() {
            override val text = "|"
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                        },
        object: BitXor() {
            override val text = "^"
            override fun calcMass(left: Mexpr, right: Mexpr) = calcMass(left, right)
                         },
        CatStr
    )

    val pool = MexprPool(items, operations, range)

    for (i in range) {
        println("$i -> ${pool.get(i)}")
    }
}

