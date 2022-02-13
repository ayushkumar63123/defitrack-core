package io.defitrack.protocol.sushiswap.pooling.polygon

import io.defitrack.abi.ABIResource
import io.defitrack.common.network.Network
import io.defitrack.polygon.config.PolygonContractAccessor
import io.defitrack.price.PriceRequest
import io.defitrack.price.PriceResource
import io.defitrack.protocol.Protocol
import io.defitrack.protocol.SushiPolygonService
import io.defitrack.protocol.reward.MiniChefV2Contract
import io.defitrack.token.Token
import io.defitrack.token.TokenType
import io.defitrack.protocol.sushiswap.apr.MinichefStakingAprCalculator
import io.defitrack.staking.StakingMarketService
import io.defitrack.staking.domain.RewardToken
import io.defitrack.staking.domain.StakedToken
import io.defitrack.staking.domain.StakingMarketElement
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode

@Component
class SushiswapPolygonStakingMinichefMarketService(
    private val abiResource: ABIResource,
    private val erC20Resource: ERC20Resource,
    private val priceResource: PriceResource,
    private val polygonContractAccessor: PolygonContractAccessor
) : StakingMarketService() {

    val minichefABI by lazy {
        abiResource.getABI("sushi/MiniChefV2.json")
    }

    override suspend fun fetchStakingMarkets(): List<StakingMarketElement> {
        return SushiPolygonService.getMiniChefs().map {
            MiniChefV2Contract(
                polygonContractAccessor,
                minichefABI,
                it
            )
        }.flatMap { chef ->
            (0 until chef.poolLength).map { poolId ->
                toStakingMarketElement(chef, poolId)
            }
        }
    }

    override fun getProtocol(): Protocol {
        return Protocol.SUSHISWAP
    }

    override fun getNetwork(): Network {
        return Network.POLYGON
    }

    private suspend fun toStakingMarketElement(
        chef: MiniChefV2Contract,
        poolId: Int
    ): StakingMarketElement {
        val stakedtoken =
            erC20Resource.getTokenInformation(getNetwork(), chef.getLpTokenForPoolId(poolId))
        val rewardToken = erC20Resource.getTokenInformation(getNetwork(), chef.rewardToken)
        return StakingMarketElement(
            id = "sushi-${chef.address}-${poolId}",
            network = getNetwork(),
            name = stakedtoken.name + " Farm",
            protocol = getProtocol(),
            token = StakedToken(
                name = stakedtoken.name,
                symbol = stakedtoken.symbol,
                address = stakedtoken.address,
                network = getNetwork(),
                decimals = stakedtoken.decimals,
                type = stakedtoken.type
            ),
            reward = listOf(
                RewardToken(
                    name = rewardToken.name,
                    symbol = rewardToken.symbol,
                    decimals = rewardToken.decimals
                )
            ),
            contractAddress = chef.address,
            vaultType = "sushi-minichefV2",
            marketSize = calculateMarketSize(chef, stakedtoken),
            rate = MinichefStakingAprCalculator(erC20Resource, priceResource, chef, poolId).calculateApr()
        )
    }

    private fun calculateMarketSize(chef: MiniChefV2Contract, stakedToken: Token): BigDecimal {
        val balance = erC20Resource.getBalance(getNetwork(), stakedToken.address, chef.address)
        return BigDecimal.valueOf(
            priceResource.calculatePrice(
                PriceRequest(
                    stakedToken.address,
                    getNetwork(),
                    balance.toBigDecimal().divide(BigDecimal.TEN.pow(stakedToken.decimals), 18, RoundingMode.HALF_UP),
                    TokenType.SUSHISWAP
                )
            )
        )
    }
}