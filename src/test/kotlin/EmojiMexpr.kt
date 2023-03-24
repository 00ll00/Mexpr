import oolloo.mexper.Mexpr
import oolloo.mexper.MexprPool
import oolloo.mexper.Op.*

/**
 * # 手语运算
 */
fun main() {
    val range = 0..100

    val items = listOf(
        Mexpr(1, "👆"),
        Mexpr(2, "✌"),
        Mexpr(5, "🖐"),
        Mexpr(6, "🤙"),
        Mexpr(10, "🤞"),
    )
    // 将括号改写为emoji
    fun Mexpr.wrapEmoji(level: Int) = if (op.level >= level) text else "👉$text👈"
    val operations = listOf(
        object: Add(){override val text = "➕"; override fun buildText(left: Mexpr, right: Mexpr) = left.wrapEmoji(3) + text + right.wrapEmoji(3)},
        object: Sub(){override val text = "➖"; override fun buildText(left: Mexpr, right: Mexpr) = left.wrapEmoji(3) + text + right.wrapEmoji(4)},
        object: Mul(){override val text = "✖"; override fun buildText(left: Mexpr, right: Mexpr) = left.wrapEmoji(5) + text + right.wrapEmoji(5)},
        object: Div(){override val text = "➗"; override fun buildText(left: Mexpr, right: Mexpr) = left.wrapEmoji(5) + text + right.wrapEmoji(6)},
    )

    val pool = MexprPool(items, operations, range)

    for (i in range) {
        println("$i -> ${pool.get(i)}")
    }
}

