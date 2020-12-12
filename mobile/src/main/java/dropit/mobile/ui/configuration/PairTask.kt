package dropit.mobile.ui.configuration

import android.util.Log
import dropit.application.client.Client
import dropit.application.client.ClientFactory
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.mobile.TAG
import dropit.mobile.domain.entity.Computer
import dropit.mobile.infrastructure.db.SQLiteHelper
import java9.util.concurrent.CompletableFuture
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class PairTask(
    private val sqLiteHelper: SQLiteHelper,
    private val clientFactory: ClientFactory,
    private val qrCodeInformation: PairingDialogFragment.QrCodeInformation,
    private val tokenRequest: TokenRequest,
    private val onSuccess: (computer: Computer) -> Unit,
    private val onFail: (throwable: Throwable) -> Unit
) : Runnable {

    @Singleton
    class Factory @Inject constructor(
        private val sqLiteHelper: SQLiteHelper,
        private val clientFactory: ClientFactory
    ) {
        fun create(
            qrCodeInformation: PairingDialogFragment.QrCodeInformation,
            tokenRequest: TokenRequest,
            onSuccess: (computer: Computer) -> Unit,
            onFail: (throwable: Throwable) -> Unit
        ): PairTask {
            return PairTask(
                sqLiteHelper, clientFactory, qrCodeInformation, tokenRequest, onSuccess, onFail
            )
        }
    }

    private var cancelled = false

    private fun selectIpAddress(): Pair<Client, String> {
        val futureClient = CompletableFuture<Pair<Client, String>>()
        val executorService = Executors.newFixedThreadPool(qrCodeInformation.ipAddresses.size)
        qrCodeInformation.ipAddresses.forEach { ipAddress ->
            executorService.submit {
                val client = clientFactory.create(
                    "https://$ipAddress:${qrCodeInformation.serverPort}",
                    tokenRequest,
                    null
                )
                val version = client.version().timeout(5, TimeUnit.SECONDS).blockingFirst()
                Log.d(this.TAG, "Connected to DropIt on $ipAddress. Version $version")
                futureClient.complete(Pair(client, ipAddress))
            }
        }
        return futureClient.get(5, TimeUnit.SECONDS)
    }

    fun cancel() {
        cancelled = true
    }

    override fun run() {
        try {
            val (client, ipAddress) = selectIpAddress()
            client.requestToken().blockingSubscribe()
            var tokenResponse = client.getTokenStatus().blockingFirst()
            while (tokenResponse.status == TokenStatus.PENDING && !cancelled) {
                Thread.sleep(500)
                tokenResponse = client.getTokenStatus().blockingFirst()
            }
            if (cancelled) return

            sqLiteHelper.insertComputer(
                Computer(
                    qrCodeInformation.computerId,
                    tokenResponse.computerSecret!!,
                    qrCodeInformation.computerName,
                    ipAddress,
                    qrCodeInformation.serverPort,
                    UUID.fromString(client.token!!),
                    tokenResponse.status
                )
            ).also(onSuccess)
        } catch (t: Throwable) {
            onFail(t)
        }
    }
}