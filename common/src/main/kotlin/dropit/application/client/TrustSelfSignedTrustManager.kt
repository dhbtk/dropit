package dropit.application.client

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class TrustSelfSignedTrustManager(private val delegate: X509TrustManager) : X509TrustManager by delegate {
    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {
        if (p0 != null && isSelfSigned(p0)) {
            return
        }

        delegate.checkServerTrusted(p0, p1)
    }

    private fun isSelfSigned(chain: Array<out X509Certificate>) = chain.size == 1
}