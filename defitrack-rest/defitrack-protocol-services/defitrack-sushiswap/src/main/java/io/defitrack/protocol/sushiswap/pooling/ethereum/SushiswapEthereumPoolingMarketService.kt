package io.defitrack.protocol.sushiswap.pooling.ethereum

import io.defitrack.common.network.Network
import io.defitrack.protocol.Protocol
import io.defitrack.protocol.SushiswapService
import io.defitrack.protocol.sushiswap.apr.SushiswapAPRService
import io.defitrack.protocol.sushiswap.pooling.DefaultSushiPoolingMarketService
import org.springframework.stereotype.Component

@Component
class SushiswapEthereumPoolingMarketService(
    sushiServices: List<SushiswapService>,
    sushiAPRService: SushiswapAPRService,
) : DefaultSushiPoolingMarketService(sushiServices, sushiAPRService) {

    override fun getProtocol(): Protocol {
        return Protocol.SUSHISWAP
    }

    override fun getNetwork(): Network {
        return Network.ETHEREUM
    }
}