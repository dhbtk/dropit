package dropit.application.client

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketListenerAdapter(
    private val onClosed: (webSocket: WebSocket, code: Int, reason: String) -> Unit = { _, _, _ -> },
    private val onClosing: (webSocket: WebSocket, code: Int, reason: String) -> Unit = { _, _, _ -> },
    private val onFailure: (webSocket: WebSocket, t: Throwable, response: Response?) -> Unit = { _, _, _ -> },
    private val onStringMessage: (webSocket: WebSocket, text: String) -> Unit = { _, _ -> },
    private val onByteMessage: (webSocket: WebSocket, bytes: ByteString) -> Unit = { _, _ -> },
    private val onOpen: (webSocket: WebSocket, response: Response) -> Unit = { _, _ -> }
) : WebSocketListener() {
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        onClosed.invoke(webSocket, code, reason)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        onClosing.invoke(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onFailure.invoke(webSocket, t, response)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onStringMessage(webSocket, text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onByteMessage(webSocket, bytes)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        onOpen.invoke(webSocket, response)
    }
}