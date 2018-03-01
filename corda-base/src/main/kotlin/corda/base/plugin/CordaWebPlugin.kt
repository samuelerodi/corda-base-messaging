package corda.base.plugin

import corda.base.api.BaseApi
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class CordaWebPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::BaseApi))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    override val staticServeDirs: Map<String, String> = mapOf(
            // This will serve the exampleWeb directory in resources to /web/example
            "corda" to javaClass.classLoader.getResource("cordaBaseWeb").toExternalForm()
    )
}
