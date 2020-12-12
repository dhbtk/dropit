package dropit.ui.image

import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.ByteArrayTransfer
import org.eclipse.swt.dnd.Transfer
import org.eclipse.swt.dnd.TransferData
import org.eclipse.swt.graphics.ImageData
import org.eclipse.swt.graphics.ImageLoader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UncheckedIOException

/**
 * Custom clipboard transfer to work around SWT bug 283960 that make copy image to clipboard not working on Linux 64.
 *
 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=283960
 *
 * @see https://stackoverflow.com/a/45819406
 */
object LinuxPngTransfer : ByteArrayTransfer() {
    private const val IMAGE_PNG = "image/png"
    private val ID = Transfer.registerType(IMAGE_PNG)

    override fun getTypeNames(): Array<String> = arrayOf(IMAGE_PNG)

    override fun getTypeIds(): IntArray = intArrayOf(ID)

    override fun javaToNative(obj: Any, transferData: TransferData) {
        if (obj !is ImageData) return
        if (!isSupportedType(transferData)) return

        try {
            ByteArrayOutputStream().use { out ->
                // write data to a byte array and then ask super to convert to pMedium

                ImageLoader().apply {
                    data = arrayOf(obj)
                    save(out, SWT.IMAGE_PNG)
                }

                super.javaToNative(out.toByteArray(), transferData)
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    override fun nativeToJava(transferData: TransferData): Any? {
        if (!isSupportedType(transferData)) return null

        val buffer = super.nativeToJava(transferData) as ByteArray
        try {
            ByteArrayInputStream(buffer).use { `in` -> return ImageData(`in`) }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }
}
