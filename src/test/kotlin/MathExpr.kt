import oolloo.mexper.Mexpr
import oolloo.mexper.MexprPool
import oolloo.mexper.Op.*

/**
 * # 使用 {2， 3， 5， 7} 生成 [[0, 100]] 内的整数
 *
 * 较为原生态的使用方式
 */
fun main() {
    val range = 0..100

    val items = arrayListOf(
        Mexpr(2, "2"),
        Mexpr(3, "3"),
        Mexpr(5, "5"),
        Mexpr(7, "7"),
    )
    val operations = arrayListOf(
        Add(),
        Sub(),
        Mul(),
        Div(),
        Mod(),
        BitAnd(),
        BitOr(),
        BitXor()
    )

    val pool = MexprPool(items, operations, range)

    for (i in range) {
        println("$i -> ${pool.get(i)}")
    }
}

