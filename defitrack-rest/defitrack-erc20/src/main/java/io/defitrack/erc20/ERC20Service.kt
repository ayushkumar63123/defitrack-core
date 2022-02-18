package io.defitrack.erc20

import io.defitrack.abi.ABIResource
import io.defitrack.common.network.Network
import io.defitrack.erc20.vo.ERC20Information
import io.defitrack.evm.contract.ERC20Contract
import io.defitrack.evm.contract.EvmContractAccessor
import io.defitrack.network.toVO
import io.defitrack.price.PriceResource
import io.github.reactivecircus.cache4k.Cache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service

@Service
class ERC20Service(
    private val abiService: ABIResource,
    private val erC20Repository: ERC20Repository,
    private val contractAccessors: List<EvmContractAccessor>,
    private val priceResource: PriceResource
) {

    val erc20Buffer = Cache.Builder().build<String, ERC20Contract>()
    val erc20ABI by lazy {
        abiService.getABI("general/ERC20.json")
    }

    fun getERC20(network: Network, address: String): ERC20? {
        val key = network.name + "-" + address.lowercase()
        return try {
            runBlocking(Dispatchers.IO) {
                erc20Buffer.get(key) {
                    ERC20Contract(
                        getContractAccessor(network),
                        erc20ABI,
                        address
                    )
                }.let {
                    ERC20(
                        name = it.name,
                        symbol = it.symbol,
                        decimals = it.decimals,
                        network = network,
                        address = address.lowercase()
                    )
                }
            }
        } catch (exception: Exception) {
            null
        }
    }

    fun getAllTokensForNetwork(network: Network): List<ERC20Information> {
        return erC20Repository.allTokens(network).mapNotNull {
            getERC20Information(network, it.address)
        }
    }

    fun getERC20Information(network: Network, address: String): ERC20Information? {

        if (address.isBlank()) {
            return null;
        }

        val correctAddress =
            (if (address == "0x0" || address == "0x0000000000000000000000000000000000000000") ERC20Repository.NATIVE_WRAP_MAPPING[network] else address)
                ?: return null

        val erc20 = getERC20(network, correctAddress)

        return erc20?.let {
            val info = erC20Repository.getToken(network, correctAddress)
            ERC20Information(
                logo = info?.logo,
                name = it.name,
                decimals = it.decimals,
                symbol = it.symbol,
                network = it.network.toVO(),
                address = it.address,
                dollarValue = priceResource.getPrice(it.symbol).toDouble()
            )
        }
    }

    fun getContractAccessor(network: Network): EvmContractAccessor {
        return contractAccessors.find {
            it.getNetwork() == network
        } ?: throw IllegalArgumentException("$network not supported")
    }

    fun getBalance(network: Network, address: String, userAddress: String) = ERC20Contract(
        getContractAccessor(network),
        erc20ABI,
        address
    ).balanceOf(userAddress)
}