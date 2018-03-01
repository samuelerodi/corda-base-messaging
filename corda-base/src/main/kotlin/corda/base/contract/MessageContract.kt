package corda.base.contract

import corda.base.state.MessageState
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
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            // Generic constraints around the IOU transaction.
            "No inputs should be consumed when issuing an IOU." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<MessageState>().single()
            "Cannot send twice to the same recipient: " + out.parties.toString()  using (out.parties.size == out.parties.toSet().size)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
    }
}
