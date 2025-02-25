package io.defitrack.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import io.defitrack.common.network.Network
import io.defitrack.protocol.sushi.domain.SushiswapPair
import io.defitrack.thegraph.TheGraphGatewayProvider
import io.github.reactivecircus.cache4k.Cache
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.days

@Component
class SpookyFantomService(
    objectMapper: ObjectMapper,
    theGraphGatewayProvider: TheGraphGatewayProvider
) : SpookyswapService {

    private val spookyService = SpookyGraphGateway(
        objectMapper,
        theGraphGatewayProvider.createTheGraphGateway("https://api.thegraph.com/subgraphs/name/sushiswap/fantom-exchange"),
    )

    fun getMasterchef() = "0x2b2929e785374c651a81a63878ab22742656dcdd"

    private val pairCache = Cache.Builder().expireAfterWrite(1.days).build<String, List<SushiswapPair>>()

    override suspend fun getPairs() = pairCache.get("all") {
        spookyService.getPairs()
    }

    override suspend fun getPairDayData(pairId: String) = spookyService.getPairDayData(pairId)

    override suspend fun getUserPoolings(user: String) = spookyService.getUserPoolings(user)

    override fun getNetwork() = Network.FANTOM
}