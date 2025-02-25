package io.defitrack.protocol.polycat.staking

import io.defitrack.abi.ABIResource
import io.defitrack.common.network.Network
import io.defitrack.common.utils.FormatUtilsExtensions.asEth
import io.defitrack.evm.contract.ContractAccessorGateway
import io.defitrack.price.PriceRequest
import io.defitrack.price.PriceResource
import io.defitrack.protocol.Protocol
import io.defitrack.protocol.polycat.PolycatMasterChefContract
import io.defitrack.protocol.polycat.PolycatService
import io.defitrack.staking.StakingMarketService
import io.defitrack.staking.domain.StakingMarket
import io.defitrack.token.ERC20Resource
import io.defitrack.token.TokenInformation
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class PolycatStakingMarketService(
    private val polycatService: PolycatService,
    private val abiResource: ABIResource,
    private val erC20Resource: ERC20Resource,
    private val priceResource: PriceResource,
    private val contractAccessorGateway: ContractAccessorGateway
) : StakingMarketService() {

    val masterChefABI by lazy {
        abiResource.getABI("polycat/MasterChef.json")
    }

    override suspend fun fetchStakingMarkets(): List<StakingMarket> {
        return polycatService.getPolycatFarms().map {
            PolycatMasterChefContract(
                contractAccessorGateway.getGateway(getNetwork()),
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
    ): StakingMarket {
        val stakedtoken =
            erC20Resource.getTokenInformation(getNetwork(), chef.poolInfo(poolId).lpToken)
        val rewardToken = erC20Resource.getTokenInformation(getNetwork(), chef.rewardToken)
        return StakingMarket(
            id = "polycat-${chef.address}-${poolId}",
            network = getNetwork(),
            name = stakedtoken.name + " Farm",
            protocol = getProtocol(),
            stakedToken = stakedtoken.toFungibleToken(),
            rewardTokens = listOf(
                rewardToken.toFungibleToken()
            ),
            apr = PolygcatStakingAprCalculator(erC20Resource, priceResource, chef, poolId).calculateApr(),
            marketSize = calculateMarketSize(stakedtoken, chef),
            contractAddress = chef.address,
            vaultType = "polycat-masterchef"
        )
    }

    private fun calculateMarketSize(stakedtoken: TokenInformation, chef: PolycatMasterChefContract): BigDecimal {

        val balance = erC20Resource.getBalance(
            getNetwork(),
            stakedtoken.address,
            chef.address
        )

        return BigDecimal(
            priceResource.calculatePrice(
                PriceRequest(
                    stakedtoken.address,
                    getNetwork(),
                    balance.asEth(stakedtoken.decimals),
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