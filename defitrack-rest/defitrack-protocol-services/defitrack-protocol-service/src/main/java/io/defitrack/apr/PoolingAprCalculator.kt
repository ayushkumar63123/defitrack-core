package io.defitrack.apr

import io.defitrack.common.utils.BigDecimalExtensions.dividePrecisely
import io.defitrack.common.utils.BigDecimalExtensions.isZero
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.time.Duration.Companion.hours

abstract class PoolingAprCalculator {

    private val cache = Cache.Builder().expireAfterWrite(10.hours).build<String, BigDecimal>()

    fun calculateApr(): BigDecimal = runBlocking {
        cache.get("apr") {
            val yearlyRewards = getYearlyRewards()
            val tvl = getTvl()
            if (yearlyRewards.isZero() || tvl.isZero()) {
                BigDecimal.ZERO
            } else {
                yearlyRewards.dividePrecisely(tvl)
            }
        }
    }

    abstract fun getYearlyRewards(): BigDecimal
    abstract fun getTvl(): BigDecimal
}