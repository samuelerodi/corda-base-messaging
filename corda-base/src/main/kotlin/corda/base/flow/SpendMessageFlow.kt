package corda.base.flow

import co.paralleluniverse.fibers.Suspendable
import corda.base.contract.MessageContract
import corda.base.contract.MessageContract.Companion.MESSAGE_CONTRACT_ID
import corda.base.flow.MessageFlow.Acceptor
import corda.base.flow.MessageFlow.Initiator
import corda.base.state.MessageState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.queryBy
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step


/**
 * This flow allows two parties (the [Initiator] and the [Acceptor]) to come to an agreement about the IOU encapsulated
 * within an [IOUState].
 *
 * In our simple example, the [Acceptor] always accepts a valid IOU.
 *
 * These flows have deliberately been implemented by using only the call() method for ease of understanding. In
 * practice we would recommend splitting up the various stages of the flow into sub-routines.
 *
 * All methods called within the [FlowLogic] sub-class need to be annotated with the @Suspendable annotation.
 */
object SpendMessageFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val linearId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {
        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
         */
        companion object {
            object GENERATING_TRANSACTION : Step("Querying message and building transaction.")
            object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
            object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {

            // Step 1. Retrieve the IOU state from the vault.
            progressTracker.currentStep = GENERATING_TRANSACTION
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
            progressTracker.currentStep = VERIFYING_TRANSACTION
            val messageToSpend = serviceHub.vaultService.queryBy<MessageState>().states.filter{ it.state.data.linearId == linearId}.single()
            progressTracker.currentStep = SIGNING_TRANSACTION
            val parties = messageToSpend.state.data.parties
            val notary = messageToSpend.state.notary


            // Generate an unsigned transaction.
            /* val messageState = MessageState(message,serviceHub.myInfo.legalIdentities.first() ,parties) */
            val txCommand = Command(MessageContract.Commands.Spend(), parties.map { it.owningKey })
            val builder = TransactionBuilder(notary)

            // Add the input IOU and IOU settle command.
            builder.addCommand(txCommand)
            builder.addInputState(messageToSpend)
            val messageSpent: MessageState = messageToSpend.state.data.burn(serviceHub.myInfo.legalIdentities.first())
            builder.addOutputState(messageSpent, MessageContract.MESSAGE_CONTRACT_ID)
            // Stage 2.

            // Verify that the transaction is valid.
            builder.verify(serviceHub)

            // Stage 3.

            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(builder)


            val partiesSessions = parties.map{initiateFlow(it)}
            // Stage 4. Send to all parties

            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, partiesSessions.toSet(), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be a Message transaction." using (output is MessageState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}
