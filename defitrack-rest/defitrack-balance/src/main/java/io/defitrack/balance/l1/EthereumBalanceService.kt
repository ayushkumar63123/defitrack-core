package io.defitrack.balance.l1

import io.defitrack.balance.BalanceService
import io.defitrack.balance.TokenBalance
import io.defitrack.common.network.Network
import io.defitrack.common.utils.BigDecimalExtensions.dividePrecisely
import io.defitrack.ethereum.config.EthereumGateway
import io.defitrack.evm.contract.ContractAccessorGateway
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Service
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigDecimal
import java.math.BigInteger

@Service
class EthereumBalanceService(
    private val contractAccessorGateway: ContractAccessorGateway,
    private val ethereumGateway: EthereumGateway,
    private val erc20Resource: ERC20Resource,
) : BalanceService {
    override fun getNetwork(): Network = Network.ETHEREUM

    override fun getNativeBalance(address: String): BigDecimal =
        ethereumGateway.web3j().ethGetBalance(address, DefaultBlockParameterName.LATEST).send().balance
            .toBigDecimal().dividePrecisely(
                BigDecimal.TEN.pow(18)
            )

    override fun getTokenBalances(user: String): List<TokenBalance> {
        val tokenAddresses = erc20Resource.getAllTokens(getNetwork()).map {
            it.address
        }

        if (tokenAddresses.isEmpty()) {
            return emptyList()
        }

        return erc20Resource.getBalancesFor(user, tokenAddresses, contractAccessorGateway.getGateway(getNetwork()))
            .mapIndexed { i, balance ->
                if (balance > BigInteger.ZERO) {
                    val token = erc20Resource.getTokenInformation(getNetwork(), tokenAddresses[i])
                    TokenBalance(
                        amount = balance,
                        token = token.toFungibleToken(),
                        network = getNetwork(),
                    )
                } else {
                    null
                }
            }.filterNotNull()
    }

    override fun nativeTokenName(): String {
        return "ETH"
    }
}