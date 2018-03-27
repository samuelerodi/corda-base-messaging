package corda.base.contract

import corda.base.state.MessageState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

/**
 * A implementation of a basic smart contract in Corda.
 *
 * This contract enforces rules regarding the creation of a valid [MessageState], which in turn encapsulates a [SignedMessage].
 *
 * For a new [SignedMessage] to be issued onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [SignedMessage].
 * - An Create() command with the public keys of both the lender and the borrower.
 *
 * All contracts must sub-class the [Contract] interface.
 */
open class MessageContract : Contract {
    companion object {
        @JvmStatic
        val MESSAGE_CONTRACT_ID = "corda.base.contract.MessageContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<MessageContract.Commands>()
        when (command.value) {
            is Commands.Create -> requireThat {
            // Generic constraints around the IOU transaction.
            "No inputs should be consumed when issuing a Message." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val output = tx.outputsOfType<MessageState>().single()
            "Cannot send twice to the same recipient: " + output.parties.toString()  using (output.parties.size == output.parties.toSet().size)
            "All of the participants must be signers." using (command.signers.containsAll(output.participants.map { it.owningKey }))
          }
          is Commands.Spend -> requireThat {
            // Generic constraints around the IOU transaction.
            "One input only should be consumed when burning a Message." using (tx.inputs.size == 1)
            "Only one output state should be created." using (tx.outputs.size == 1)
            val input = tx.inputsOfType<MessageState>().single()
            "Cannot burn an already burnt message" using (input.consumer == null)
            val output = tx.outputsOfType<MessageState>().single()
            "Consumer is required" using (output.consumer != null)
            "Issuer can't be modified" using (output.issuer == input.issuer)
            "Parties must not be changed" using (output.parties == input.parties)
            "Message can't be modified" using (output.message == input.message)
            /* "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey })) */
        }
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Spend : TypeOnlyCommandData(), Commands
    }
}
