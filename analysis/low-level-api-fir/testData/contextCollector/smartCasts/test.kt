fun main(args: Array<String>) {
    val b: Testtt? = Testtt()

    if (<expr>b?.a</expr> != null && b.a.a != null) {

    }
}

class Testtt {
    val a: Testtt? = null
}