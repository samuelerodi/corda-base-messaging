package corda.base.state

import corda.base.schema.MessageSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param message the message that each party will sign.
 * @param parties the party receiving and approving the message.
 */
data class MessageState(val message: String,
                        val issuer: Party,
                        val parties: List<Party>,
                        override val linearId: UniqueIdentifier = UniqueIdentifier()):
    LinearState, QueryableState {

    var consumer: Party?=null;

    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = parties + listOf(issuer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is MessageSchemaV1 -> MessageSchemaV1.PersistentMessage(
                    this.parties.map { it.name.toString() }.toString(),
                    this.issuer.toString(),
                    this.message,
                    this.consumer?.toString(),
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(MessageSchemaV1)
    public fun burn(consumer: Party):MessageState {var cp=copy(); cp.consumer=consumer; return cp} 
}
