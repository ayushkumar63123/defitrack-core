package io.defitrack.protocol

enum class Protocol(
    val logo: String,
    val slug: String,
    val enabled: Boolean = true
) {

    AAVE("aave.png", "aave"),
    HUMANDAO("humandao.jpg", "humandao"),
    CURVE("curve.png", "curve"),
    MSTABLE("mstable.png", "mstable"),
    COMPOUND("compound.png", "compound"),
    BEEFY("beefy.png", "beefy"),
    QUICKSWAP("quickswap.png", "quickswap"),
    POLYCAT("polycat.webp", "polycat"),
    HOP("hop.jpg", "hop"),
    DINOSWAP("dinoswap.png", "dinoswap"),
    ADAMANT("adamant.png", "adamant"),
    UNISWAP("uniswap.png", "uniswap"),
    DFYN("dfyn.svg", "dfyn"),
    IDEX("idex.png", "idex"),
    SUSHISWAP("sushiswap.png", "sushiswap"),
    DMM("dmm.png", "dmm"),
    BALANCER("balancer.png", "balancer"),
    STARGATE("stargate.png", "stargate"),
    SPOOKY("spooky.png", "spooky"),
    SPIRITSWAP("spirit.png", "spirit"),
    CONVEX("convex.png", "convex");

    val imageBasePath = "https://static.defitrack.io/images/protocols/"

    fun getImage(): String = imageBasePath + logo
}