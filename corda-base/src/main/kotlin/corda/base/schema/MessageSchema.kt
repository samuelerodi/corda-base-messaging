package corda.base.schema


import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.OneToMany
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object MessageSchema

/**
 * An IOUState schema.
 */
object MessageSchemaV1 : MappedSchema(
        schemaFamily = MessageSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentMessage::class.java)) {
    @Entity
    @Table(name = "message_states")
    class PersistentMessage(
            @Column(name = "parties")
            var parties: String,

            @Column(name = "issuer")
            var issuer: String,

            @Column(name = "message")
            var message: String,

            @Column(name = "consumer")
            var consumer: String?,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
    constructor(): this("", "", "", "",  UUID.randomUUID())
    }
}
