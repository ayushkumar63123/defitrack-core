package io.defitrack.protocol.quickswap.staking

import io.defitrack.abi.ABIResource
import io.defitrack.common.network.Network
import io.defitrack.polygon.config.PolygonContractAccessor
import io.defitrack.price.PriceRequest
import io.defitrack.price.PriceResource
import io.defitrack.protocol.Protocol
import io.defitrack.protocol.quickswap.QuickswapDualRewardPoolContract
import io.defitrack.protocol.quickswap.apr.QuickswapAPRService
import io.defitrack.protocol.staking.Token
import io.defitrack.quickswap.QuickswapService
import io.defitrack.staking.StakingMarketService
import io.defitrack.staking.domain.StakingMarketElement
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class QuickswapDualStakingMarketService(
    private val quickswapService: QuickswapService,
    private val polygonContractAccessor: PolygonContractAccessor,
    private val abiService: ABIResource,
    private val priceResource: PriceResource,
    private val quickswapAPRService: QuickswapAPRService,
    private val erC20Resource: ERC20Resource,
) : StakingMarketService() {

    val stakingRewardsABI by lazy {
        abiService.getABI("quickswap/DualStakingRewards.json")
    }

    override suspend fun fetchStakingMarkets(): List<StakingMarketElement> {
        return quickswapService.getDualPools().map {
            QuickswapDualRewardPoolContract(
                polygonContractAccessor,
                stakingRewardsABI,
                it
            )
        }.mapNotNull { pool ->
            try {
                val stakedToken = erC20Resource.getTokenInformation(getNetwork(), pool.stakingTokenAddress)
                val rewardTokenA = erC20Resource.getTokenInformation(getNetwork(), pool.rewardsTokenAddressA)
                val rewardTokenB = erC20Resource.getTokenInformation(getNetwork(), pool.rewardsTokenAddressB)

                StakingMarketElement(
                    id = "quickswap-polygon-dual-${pool.address}",
                    network = getNetwork(),
                    protocol = getProtocol(),
                    name = "${stakedToken.name} Dual Reward Pool",
                    token = stakedToken.toStakedToken(),
                    reward = listOf(
                        rewardTokenA.toRewardToken(),
                        rewardTokenB.toRewardToken()
                    ),
                    contractAddress = pool.address,
                    vaultType = "quickswap-dual-reward-pool",
                    marketSize = getMarketSize(stakedToken, pool),
                    rate = getApr(pool, stakedToken)
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }
    }

    private fun getApr(
        pool: QuickswapDualRewardPoolContract,
        stakedToken: Token
    ): BigDecimal {

        return (quickswapAPRService.getDualPoolAPR(pool.address) + quickswapAPRService.getLPAPR(
            stakedToken.address
        ))
    }

    private fun getMarketSize(
        stakedToken: Token,
        pool: QuickswapDualRewardPoolContract
    ) = BigDecimal.valueOf(
        priceResource.calculatePrice(
            PriceRequest(
                address = stakedToken.address,
                network = getNetwork(),
                amount = pool.totalSupply.toBigDecimal().divide(
                    BigDecimal.TEN.pow(stakedToken.decimals), RoundingMode.HALF_UP
                ),
                type = stakedToken.type
            )
        )
    )

    override fun getProtocol(): Protocol {
        return Protocol.QUICKSWAP
    }

    override fun getNetwork(): Network {
        return Network.POLYGON
    }
}