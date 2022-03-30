package io.defitrack.bsc

import io.defitrack.evm.abi.AbiDecoder
import io.defitrack.common.network.Network
import io.defitrack.evm.contract.EvmContractAccessor
import io.defitrack.evm.web3j.EvmGateway
import io.ktor.client.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class BscContractAccessorConfig(
    private val abiDecoder: AbiDecoder,
    private val httpClient: HttpClient,
    @Value("\${io.defitrack.services.bsc.endpoint:http://defitrack-bsc:8080}") private val endpoint: String,
)  {

    @Bean
    fun bscContractAccessor(): EvmContractAccessor {
        return EvmContractAccessor(
            abiDecoder,
            Network.BSC,
            "0x41263cba59eb80dc200f3e2544eda4ed6a90e76c",
            httpClient,
            endpoint
        )
    }
}