package dropit.mobile.ui.configuration

import android.util.Log
import dropit.application.client.Client
import dropit.application.client.ClientFactory
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.mobile.TAG
import dropit.mobile.application.entity.Computer
import dropit.mobile.lib.db.SQLiteHelper
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java9.util.concurrent.CompletableFuture
import java.util.*
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
        val subscriptions = qrCodeInformation.ipAddresses.map { ipAddress ->
            val client = clientFactory.create(
                "https://$ipAddress:${qrCodeInformation.serverPort}",
                tokenRequest,
                null
            )
            client.version()
                .subscribeOn(Schedulers.newThread())
                .timeout(5, TimeUnit.SECONDS)
                .subscribe { version, exception ->
                    if (version != null) {
                        Log.d(this.TAG, "Connected to DropIt on $ipAddress. Version $version")
                        futureClient.complete(Pair(client, ipAddress))
                    } else {
                        Log.d(this.TAG, "Failed to connect: ${exception.message}")
                        exception.printStackTrace()
                    }
                }
        }
        return futureClient.get(5, TimeUnit.SECONDS).also {
            subscriptions.forEach(Disposable::dispose)
        }
    }

    fun cancel() {
        cancelled = true
    }

    override fun run() {
        try {
            val (client, ipAddress) = selectIpAddress()
            client.requestToken().ignoreElement().blockingAwait()
            var tokenResponse = client.getTokenStatus().blockingGet()
            while (tokenResponse.status == TokenStatus.PENDING && !cancelled) {
                Thread.sleep(500)
                tokenResponse = client.getTokenStatus().blockingGet()
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