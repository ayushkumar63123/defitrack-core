package io.defitrack.protocol.uniswap.pooling

import io.defitrack.pool.StandardLpPositionProvider
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Service

@Service
class UniswapUserPoolingService(
    ethereumPoolingMarketService: UniswapEthereumPoolingMarketService,
    erC20Resource: ERC20Resource
) : StandardLpPositionProvider(ethereumPoolingMarketService, erC20Resource)