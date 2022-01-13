package io.defitrack.pool

import io.defitrack.common.network.Network
import io.defitrack.logo.LogoService
import io.defitrack.network.toVO
import io.defitrack.pool.domain.PoolingMarketElement
import io.defitrack.pool.vo.PoolingMarketElementToken
import io.defitrack.pool.vo.PoolingMarketElementVO
import io.defitrack.protocol.staking.LpToken
import io.defitrack.protocol.toVO
import io.defitrack.token.ERC20Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pooling")
class DefaultPoolingMarketRestController(
    private val poolingMarketServices: List<PoolingMarketService>,
    private val logoService: LogoService,
    private val erC20Resource: ERC20Resource
) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }

    @GetMapping(value = ["/all-markets"])
    fun allMarkets(): List<PoolingMarketElement> {
        return poolingMarketServices.flatMap {
            it.getPoolingMarkets()
        }
    }

    @GetMapping(value = ["/markets"], params = ["token", "network"])
    fun searchByToken(
        @RequestParam("token") tokenAddress: String,
        @RequestParam("network") network: Network
    ): List<PoolingMarketElementVO> {
        return poolingMarketServices
            .filter {
                it.getNetwork() == network
            }
            .flatMap {
                it.getPoolingMarkets()
            }.filter {
                it.token.any { t ->
                    t.address.lowercase() == tokenAddress.lowercase()
                } || it.address.lowercase() == tokenAddress.lowercase()
            }.map { poolingMarketElementVO(it) }
    }

    @GetMapping(value = ["/markets/{id}"], params = ["network"])
    fun getById(
        @PathVariable("id") id: String,
        @RequestParam("network") network: Network
    ): ResponseEntity<PoolingMarketElementVO> {
        return poolingMarketServices
            .filter {
                it.getNetwork() == network
            }.flatMap {
                it.getPoolingMarkets()
            }.firstOrNull {
                it.id == id
            }?.let {
                ResponseEntity.ok(poolingMarketElementVO(it))
            } ?: ResponseEntity.notFound().build()
    }

    @GetMapping(value = ["/markets/alternatives"], params = ["token", "network"])
    fun findAlternatives(
        @RequestParam("token") tokenAddress: String,
        @RequestParam("network") network: Network
    ): List<PoolingMarketElementVO> {
        val token = erC20Resource.getTokenInformation(
            network, tokenAddress,
        )
        return poolingMarketServices
            .filter {
                it.getNetwork() == network
            }
            .flatMap {
                it.getPoolingMarkets()
            }.filter { poolingMarketElement ->
                when (token) {
                    is LpToken -> {
                        poolingMarketElement.token.map { pt ->
                            pt.address.lowercase()
                        }.containsAll(listOf(token.token0.address.lowercase(), token.token1.address.lowercase()))
                    }
                    else -> false
                }
            }.map { poolingMarketElementVO(it) }
    }

    private fun poolingMarketElementVO(it: PoolingMarketElement) =
        PoolingMarketElementVO(
            name = it.name,
            protocol = it.protocol.toVO(),
            network = it.network.toVO(),
            token = it.token.map { token ->
                PoolingMarketElementToken(
                    name = token.name,
                    symbol = token.symbol,
                    address = token.address,
                    logo = logoService.generateLogoUrl(it.network, token.address),
                )
            },
            id = it.id,
            address = it.address,
            apr = it.apr,
            marketSize = it.marketSize
        )
}