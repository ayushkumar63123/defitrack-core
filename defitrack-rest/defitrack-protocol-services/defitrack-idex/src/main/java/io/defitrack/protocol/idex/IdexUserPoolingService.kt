package io.defitrack.protocol.idex

import io.defitrack.pool.StandardLpPositionProvider
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Component

@Component
class IdexUserPoolingService(
    idexPoolingMarketService: IdexPoolingMarketService,
    erC20Resource: ERC20Resource
) : StandardLpPositionProvider(idexPoolingMarketService, erC20Resource)