package io.defitrack.staking

import io.defitrack.common.network.Network
import io.defitrack.network.toVO
import io.defitrack.token.TokenType
import io.defitrack.protocol.toVO
import io.defitrack.staking.domain.StakingMarketElement
import io.defitrack.staking.vo.RewardTokenVO
import io.defitrack.staking.vo.StakedTokenVO
import io.defitrack.staking.vo.StakingMarketElementVO
import io.defitrack.token.ERC20Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/staking")
class DefaultStakingMarketRestController(
    private val stakingMarketServices: List<StakingMarketService>,
    private val erC20Resource: ERC20Resource
) {

    @GetMapping("/all-markets", params = ["network"])
    fun searchByToken(
        @RequestParam("network") network: Network
    ): List<StakingMarketElementVO> {
        return stakingMarketServices
            .filter {
                it.getNetwork() == network
            }.flatMap {
                it.getStakingMarkets()
            }.map {
                toVO(it)
            }
    }

    @GetMapping(value = ["/markets"], params = ["token", "network"])
    fun searchByToken(
        @RequestParam("token") tokenAddress: String,
        @RequestParam("network") network: Network
    ): List<StakingMarketElementVO> {
        return stakingMarketServices
            .filter {
                it.getNetwork() == network
            }.flatMap {
                it.getStakingMarkets()
            }.filter {
                val token = erC20Resource.getTokenInformation(it.network, it.token.address.lowercase())
                if (token.type != TokenType.SINGLE) {
                    token.token0!!.address.lowercase() == tokenAddress.lowercase() || token.token1!!.address.lowercase() == tokenAddress.lowercase() || it.token.address.lowercase() == tokenAddress.lowercase()
                } else {
                    it.token.address.lowercase() == tokenAddress.lowercase()
                }
            }.map {
                toVO(it)
            }
    }

    @GetMapping(value = ["/markets/{id}"], params = ["network"])
    fun getById(
        @PathVariable("id") id: String,
        @RequestParam("network") network: Network
    ): ResponseEntity<StakingMarketElementVO> {
        return stakingMarketServices
            .filter {
                it.getNetwork() == network
            }.flatMap {
                it.getStakingMarkets()
            }.firstOrNull {
                it.id == id
            }?.let {
                ResponseEntity.ok(toVO(it))
            } ?: ResponseEntity.notFound().build()
    }

    private fun toVO(it: StakingMarketElement) = StakingMarketElementVO(
        id = it.id,
        network = it.network.toVO(),
        protocol = it.protocol.toVO(),
        name = it.name,
        stakedToken = StakedTokenVO(
            name = it.token.name,
            symbol = it.token.symbol,
            address = it.token.address,
            network = it.token.network.toVO(),
            decimals = it.token.decimals,
            type = it.token.type
        ),
        reward = it.reward.map { reward ->
            RewardTokenVO(
                name = reward.name,
                symbol = reward.symbol,
                decimals = reward.decimals
            )
        },
        contractAddress = it.contractAddress,
        vaultType = it.vaultType,
        marketSize = it.marketSize,
        rate = it.rate
    )
}