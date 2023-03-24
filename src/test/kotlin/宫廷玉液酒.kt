import oolloo.mexper.Mexpr
import oolloo.mexper.MexprPool
import oolloo.mexper.Op.*

/**
 * # 宫廷玉液酒，一百八一杯
 */
fun main() {
    val range = 0..100

    val items = listOf(
        Mexpr(40, "小锤"),
        Mexpr(80, "大锤"),
        Mexpr(180, "宫廷玉液酒"),
    )
    val operations = listOf(
        object: Add(){override val text = "加"},
        object: Sub(){override val text = "减"},
        object: Mul(){override val text = "乘"},
        object: Div(){override val text = "除以"},
        object: Mod(){override val text = "模"},
    )

    val pool = MexprPool(items, operations, range)

    for (i in range) {
        println("$i -> ${pool.get(i)}")
    }
}

