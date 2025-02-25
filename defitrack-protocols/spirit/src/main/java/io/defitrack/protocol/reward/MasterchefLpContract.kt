package io.defitrack.protocol.reward

import io.defitrack.evm.contract.BlockchainGateway
import io.defitrack.evm.contract.BlockchainGateway.Companion.toAddress
import io.defitrack.evm.contract.BlockchainGateway.Companion.toUint256
import io.defitrack.evm.contract.EvmContract
import io.defitrack.evm.contract.multicall.MultiCallElement
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint16
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

class MasterchefLpContract(
    blockchainGateway: BlockchainGateway,
    abi: String,
    address: String
) : EvmContract(
    blockchainGateway, abi, address
) {

    val poolLength by lazy {
        (read(
            "poolLength",
            outputs = listOf(TypeReference.create(Uint256::class.java))
        )[0].value as BigInteger).toInt()
    }

    val totalAllocPoint by lazy {
        (read(
            "totalAllocPoint",
            outputs = listOf(TypeReference.create(Uint256::class.java))
        )[0].value as BigInteger)
    }

    fun getLpTokenForPoolId(poolId: Int): String {
        return poolInfos[poolId].lpToken
    }

    val rewardToken by lazy {
        read(
            "spirit",
            outputs = listOf(TypeReference.create(Address::class.java))
        )[0].value as String
    }

    val sushiPerSecond by lazy {
        read(
            "spiritPerBlock",
            outputs = listOf(TypeReference.create(Uint256::class.java))
        )[0].value as BigInteger
    }

    fun userInfoFunction(poolId: Int, user: String): Function {
        return createFunction(
            "userInfo",
            listOf(
                poolId.toBigInteger().toUint256(),
                user.toAddress()
            ),
            listOf(
                TypeReference.create(Uint256::class.java),
                TypeReference.create(Uint256::class.java)
            )
        )
    }


    val poolInfos: List<PoolInfo> by lazy {
        val multicalls = (0 until poolLength).map { poolIndex ->
            MultiCallElement(
                createFunction(
                    "poolInfo",
                    inputs = listOf(poolIndex.toBigInteger().toUint256()),
                    outputs = listOf(
                        TypeReference.create(Address::class.java),
                        TypeReference.create(Uint256::class.java),
                        TypeReference.create(Uint256::class.java),
                        TypeReference.create(Uint256::class.java),
                        TypeReference.create(Uint16::class.java),
                    )
                ),
                this.address
            )
        }

        val results = this.blockchainGateway.readMultiCall(
            multicalls
        )
        results.map { retVal ->
            PoolInfo(
                retVal[0].value as String,
                retVal[1].value as BigInteger,
                retVal[2].value as BigInteger,
                retVal[3].value as BigInteger,
                retVal[4].value as BigInteger,
            )
        }
    }

    fun poolInfo(poolIndex: Int): PoolInfo {
        return poolInfos[poolIndex]
    }
}