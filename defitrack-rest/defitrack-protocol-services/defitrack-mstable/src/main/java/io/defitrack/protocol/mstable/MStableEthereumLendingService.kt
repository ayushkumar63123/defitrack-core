package io.defitrack.protocol.mstable

import io.defitrack.abi.ABIResource
import io.defitrack.common.network.Network
import io.defitrack.ethereum.config.EthereumContractAccessor
import io.defitrack.lending.LendingService
import io.defitrack.lending.domain.LendingElement
import io.defitrack.protocol.Protocol
import io.defitrack.token.ERC20Resource
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.*

@Service
class MStableEthereumLendingService(
    private val mStableService: MStableEthereumService,
    private val abiResource: ABIResource,
    private val erC20Resource: ERC20Resource,
    private val ethereumContractAccessor: EthereumContractAccessor,
) : LendingService {

    val savingsContractABI by lazy {
        abiResource.getABI("mStable/SavingsContract.json")
    }

    override fun getProtocol(): Protocol {
        return Protocol.MSTABLE
    }

    override fun getNetwork(): Network {
        return Network.ETHEREUM
    }

    override suspend fun getLendings(address: String): List<LendingElement> {
        val contracts = mStableService.getSavingsContracts().map {
            MStableEthereumSavingsContract(
                ethereumContractAccessor,
                savingsContractABI,
                it
            )
        }

        return erC20Resource.getBalancesFor(address, contracts.map { it.address }, ethereumContractAccessor)
            .mapIndexed { index, balance ->
                if (balance > BigInteger.ZERO) {
                    val contract = contracts[index]

                    val lendingToken = erC20Resource.getTokenInformation(getNetwork(), contract.address)
                    LendingElement(
                        id = UUID.randomUUID().toString(),
                        network = getNetwork(),
                        protocol = getProtocol(),
                        name = contract.name,
                        amount = balance,
                        token = lendingToken.toFungibleToken()
                    )
                } else {
                    null
                }
            }.filterNotNull()
    }
}