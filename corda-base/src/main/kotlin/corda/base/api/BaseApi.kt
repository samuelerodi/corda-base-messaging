package corda.base.api

import corda.base.flow.MessageFlow
import corda.base.flow.MessageFlow.Initiator
import corda.base.state.MessageState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.BAD_REQUEST
import javax.ws.rs.core.Response.Status.CREATED

val SERVICE_NAMES = listOf("Controller", "Network Map Service")

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("corda")
class BaseApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<BaseApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }


    /**
     * Display corda node info.
     */
    @GET
    @Path("nodeInfo")
    @Produces(MediaType.APPLICATION_JSON)
    fun getNodeInfo() = mapOf(
            "legalIdentities" to rpcOps.nodeInfo().legalIdentities.map{ it.name },
            "addresses" to rpcOps.nodeInfo().addresses.map{it.host + ":" + it.port},
            "platformVersion" to rpcOps.nodeInfo().platformVersion,
            "serial" to rpcOps.nodeInfo().serial,
            "peers" to rpcOps.networkMapSnapshot().map { it.legalIdentities.first().name.toString() + " -> " + it.addresses.toString()}
    )

    /**
     * Display corda platform version.
     */
    @GET
    @Path("version")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCordaVersion() = rpcOps.nodeInfo().platformVersion


    /**
     * Displays all states that exist in the node's vault.
     */
    @GET
    @Path("dbstatus")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDBStatus() = rpcOps.vaultQueryBy<MessageState>().states

    /**
     * Displays all states that exist in the node's vault.
     */
    @GET
    @Path("message")
    @Produces(MediaType.APPLICATION_JSON)
    fun getMessage() = rpcOps.vaultQueryBy<MessageState>().states

    /**
     * Displays all registered flows that exist in the node's.
     */
    @GET
    @Path("flows")
    @Produces(MediaType.APPLICATION_JSON)
    fun getRegisteredFlows() = mapOf("flows" to rpcOps.registeredFlows())

    /**
     * Displays all registered flows that exist in the node's.
     */
    @PUT
    @Path("message")
    fun sendMessage(@QueryParam(value = "message") message: String,
                    @QueryParam(value = "parties") partyList: List<String>): Response {
        // Get party objects for myself and the counterparty.
        val parties  : MutableList<Party> = mutableListOf()
        try {
            if (partyList.isNotEmpty()){
                val temp = partyList
                        .map{rpcOps.wellKnownPartyFromX500Name(CordaX500Name.parse(it)) ?: throw IllegalArgumentException("Unknown party name.")}
                parties.addAll(temp)
            }
            if (message == null || message == "") throw IllegalArgumentException("Message not specified.")

            // Start the MessageFlow. We block and waits for the flow to return.
            val result = rpcOps.startTrackedFlow(::Initiator, message, parties).returnValue.get()
            // Return the response.
            return Response
                    .status(Response.Status.CREATED)
                    .entity("Transaction id ${result.id} committed to ledger.\n${result.tx.outputs.single()}")
                    .build()
        }
        catch (e: Exception) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.message)
                    .build()
        }

    }

}
