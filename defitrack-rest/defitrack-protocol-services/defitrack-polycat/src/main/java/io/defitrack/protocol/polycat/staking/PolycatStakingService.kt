package io.defitrack.protocol.polycat.staking

import com.fasterxml.jackson.databind.ObjectMapper
import io.defitrack.abi.ABIResource
import io.defitrack.common.network.Network
import io.defitrack.ethereumbased.contract.ERC20Contract
import io.defitrack.polycat.PolycatMasterChefContract
import io.defitrack.polycat.PolycatService
import io.defitrack.polygon.config.PolygonContractAccessor
import io.defitrack.protocol.Protocol
import io.defitrack.staking.UserStakingService
import io.defitrack.staking.domain.StakingElement
import io.defitrack.staking.domain.VaultRewardToken
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

@Service
class PolycatStakingService(
    private val polycatService: PolycatService,
    private val abiResource: ABIResource,
    objectMapper: ObjectMapper,
    erC20Resource: ERC20Resource,
    private val polygonContractAccessor: PolygonContractAccessor,
) : UserStakingService(erC20Resource, objectMapper) {

    val masterChefABI by lazy {
        abiResource.getABI("polycat/MasterChef.json")
    }

    override fun getStakings(address: String): List<StakingElement> {

        val polycatMasterChefContracts = polycatService.getPolycatFarms().map {
            PolycatMasterChefContract(
                polygonContractAccessor,
                masterChefABI,
                it
            )
        }

        return polycatMasterChefContracts.flatMap { masterChef ->
            (0 until masterChef.poolLength).mapNotNull { poolIndex ->
                val balance = masterChef.userInfo(address, poolIndex).amount

                if (balance > BigInteger.ZERO) {
                    val stakedtoken =
                        erC20Resource.getTokenInformation(getNetwork(), masterChef.getLpTokenForPoolId(poolIndex))
                    val rewardToken = erC20Resource.getTokenInformation(getNetwork(), masterChef.rewardToken)

                    val poolBalance = ERC20Contract(
                        polygonContractAccessor,
                        abiResource.getABI("general/ERC20.json"),
                        stakedtoken.address
                    ).balanceOf(masterChef.address)


                    val userRewardPerBlock = balance.toBigDecimal().divide(
                        poolBalance.toBigDecimal(), 18, RoundingMode.HALF_UP
                    ).times(masterChef.rewardPerBlock.toBigDecimal())

                    val perDay = userRewardPerBlock.times(BigDecimal(43200))

                    StakingElement(
                        id = "polycat-${masterChef.address}-${poolIndex}",
                        network = getNetwork(),
                        user = address.lowercase(),
                        protocol = getProtocol(),
                        name = stakedtoken.name + " Vault",
                        url = "https://polygon.iron.finance/farms",
                        stakedToken = vaultStakedToken(
                            stakedtoken.address,
                            balance
                        ),
                        rewardTokens = listOf(
                            VaultRewardToken(
                                name = rewardToken.name,
                                symbol = rewardToken.symbol,
                                decimals = rewardToken.decimals,
                                daily = perDay.divide(BigDecimal.TEN.pow(18), 4, RoundingMode.HALF_UP).toString()
                            )
                        ),
                        contractAddress = masterChef.address,
                        vaultType = "polycat-masterchef"
                    )
                } else {
                    null
                }
            }
        }
    }

    override fun getProtocol(): Protocol {
        return Protocol.POLYCAT
    }

    override fun getNetwork(): Network {
        return Network.POLYGON
    }
}