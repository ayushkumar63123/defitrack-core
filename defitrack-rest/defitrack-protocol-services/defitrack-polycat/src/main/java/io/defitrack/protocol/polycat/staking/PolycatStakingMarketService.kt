package io.defitrack.protocol.polycat.staking

import io.defitrack.abi.ABIResource
import io.defitrack.common.network.Network
import io.defitrack.common.utils.FormatUtilsExtensions.asEth
import io.defitrack.polygon.config.PolygonContractAccessor
import io.defitrack.price.PriceRequest
import io.defitrack.price.PriceResource
import io.defitrack.protocol.Protocol
import io.defitrack.protocol.polycat.PolycatMasterChefContract
import io.defitrack.protocol.polycat.PolycatService
import io.defitrack.token.Token
import io.defitrack.staking.StakingMarketService
import io.defitrack.staking.domain.RewardToken
import io.defitrack.staking.domain.StakedToken
import io.defitrack.staking.domain.StakingMarketElement
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PolycatStakingMarketService(
    private val polycatService: PolycatService,
    private val abiResource: ABIResource,
    private val erC20Resource: ERC20Resource,
    private val priceResource: PriceResource,
    private val polygonContractAccessor: PolygonContractAccessor,
) : StakingMarketService() {

    val masterChefABI by lazy {
        abiResource.getABI("polycat/MasterChef.json")
    }

    override suspend fun fetchStakingMarkets(): List<StakingMarketElement> {
        return polycatService.getPolycatFarms().map {
            PolycatMasterChefContract(
                polygonContractAccessor,
                masterChefABI,
                it
            )
        }.flatMap { chef ->
            (0 until chef.poolLength).map { poolId ->
                toStakingMarketElement(chef, poolId)
            }
        }
    }

    private fun toStakingMarketElement(
        chef: PolycatMasterChefContract,
        poolId: Int
    ): StakingMarketElement {
        val stakedtoken =
            erC20Resource.getTokenInformation(getNetwork(), chef.poolInfo(poolId).lpToken)
        val rewardToken = erC20Resource.getTokenInformation(getNetwork(), chef.rewardToken)
        return StakingMarketElement(
            id = "polycat-${chef.address}-${poolId}",
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
            rate = PolygcatStakingAprCalculator(erC20Resource, priceResource, chef, poolId).calculateApr(),
            marketSize = calculateMarketSize(stakedtoken, chef),
            contractAddress = chef.address,
            vaultType = "polycat-masterchef"
        )
    }

    private fun calculateMarketSize(stakedtoken: Token, chef: PolycatMasterChefContract): BigDecimal {

        val balance = erC20Resource.getBalance(
            getNetwork(),
            stakedtoken.address,
            chef.address
        );

        return BigDecimal(
            priceResource.calculatePrice(
                PriceRequest(
                    stakedtoken.address,
                    getNetwork(),
                    balance.asEth(stakedtoken.decimals).toBigDecimal(),
                    stakedtoken.type
                )
            )
        )
    }


    override fun getProtocol(): Protocol {
        return Protocol.POLYCAT
    }

    override fun getNetwork(): Network {
        return Network.POLYGON
    }
}