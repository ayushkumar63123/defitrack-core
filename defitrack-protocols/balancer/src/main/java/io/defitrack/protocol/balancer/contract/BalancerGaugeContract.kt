package io.defitrack.protocol.balancer.contract

import io.defitrack.evm.contract.BlockchainGateway
import io.defitrack.evm.contract.BlockchainGateway.Companion.toAddress
import io.defitrack.evm.contract.BlockchainGateway.Companion.toUint256
import io.defitrack.evm.contract.ERC20Contract
import io.defitrack.evm.contract.multicall.MultiCallElement
import io.defitrack.token.FungibleToken
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

class BalancerGaugeContract(
    blockchainGateway: BlockchainGateway,
    abi: String,
    address: String
) : ERC20Contract(blockchainGateway, abi, address) {

    fun getClaimableRewardFunction(address: String, token: String): Function {
        return createFunction(
            "claimable_reward_write",
            listOf(
                address.toAddress(),
                token.toAddress()
            ),
            listOf(
                TypeReference.create(Uint256::class.java)
            )
        )
    }

    fun getClaimRewardsFunction(): Function {
        return createFunction(
            "claim_rewards",
            emptyList(),
            emptyList()
        )
    }

    fun getBalances(user: String, rewardTokens: List<FungibleToken>): List<BalancerGaugeBalance> {
        return blockchainGateway.readMultiCall(
            rewardTokens.map { token ->
                MultiCallElement(
                    getClaimableRewardFunction(user, token.address),
                    this.address
                )
            }
        ).mapIndexed { index, retVal ->
            val token = rewardTokens[index]
            BalancerGaugeBalance(
                token = token,
                retVal[0].value as BigInteger
            )
        }
    }

    class BalancerGaugeBalance(
        val token: FungibleToken,
        val balance: BigInteger
    )

    fun getRewardTokens(): List<String> {
        return (0..3).mapNotNull {
            try {
                val rewardToken = getRewardToken(it)
                if (rewardToken != "0x0000000000000000000000000000000000000000") {
                    rewardToken
                } else {
                    null
                }
            } catch (ex: Exception) {
                null
            }
        }
    }


    fun getRewardToken(index: Int): String {
        return read(
            "reward_tokens",
            listOf(index.toBigInteger().toUint256()),
            listOf(TypeReference.create(Address::class.java))
        )[0].value as String
    }
}