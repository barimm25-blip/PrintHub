package com.example.helloapk

import android.R
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.helloapk.ui.theme.HelloApkTheme
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.net.Socket
import android.graphics.RectF
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.random.Random
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.withContext



import kotlinx.coroutines.withContext

import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        enableEdgeToEdge()
        setContent {
            HelloApkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScanScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}



@Composable
fun ScanScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var text by remember { mutableStateOf("") }
    var lastScanned by remember { mutableStateOf("") }
    var handledByEnterOrTab by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.hide()
    }


    LaunchedEffect(text) {
        if (text.isEmpty()) return@LaunchedEffect
        delay(250)
        if (!handledByEnterOrTab && text.isNotEmpty()) {
            val code = sanitize(text)
            lastScanned = code
            processScan(context, code)
            text = ""
            focusRequester.requestFocus()
            keyboard?.hide()
        }
        handledByEnterOrTab = false
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            label = { Text("Scan ") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (text.isNotBlank()) {
                        handledByEnterOrTab = true
                        val code = sanitize(text)
                        lastScanned = code
                        processScan(context, code)
                        text = ""
                        keyboard?.hide()
                        focusRequester.requestFocus()
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown &&
                        (it.key == Key.Enter || it.key == Key.Tab)
                    ) {
                        if (text.isNotBlank()) {
                            handledByEnterOrTab = true
                            val code = sanitize(text)
                            lastScanned = code
                            processScan(context, code)
                            text = ""
                            keyboard?.hide()
                            focusRequester.requestFocus()
                        }
                        true
                    } else false
                }
        )


        Spacer(Modifier.height(12.dp))

        Text(text = "Text : ${if (lastScanned.isEmpty()) "-" else lastScanned}")

        if (lastScanned.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))

            val qrBitmap = remember(lastScanned) {
                generateQrBitmap(lastScanned, size = 512)
            }
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR Code of $lastScanned",
                modifier = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(16.dp))

            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    if (lastScanned.isEmpty()) return@Button


                    scope.launch {

                        val pdf = createA4PdfWithQr(context, qrBitmap, "qr_to_print_a4.pdf")
                        if (pdf == null) {
                            Toast.makeText(context, "สร้าง PDF ไม่สำเร็จ", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        try {
                            // 2) ส่ง PDF ไปที่ Print Server ผ่าน LPR
                            printPdfViaLpr(
                                ip = "xxx.xxx.xxx.xxx",
                                queue = "PS-CED0C4",   // ← ใส่ชื่อแชร์จริงของเครื่องพิมพ์บนเซิร์ฟเวอร์
                                pdfFile = pdf
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "ส่งงานพิมพ์ไปที่เซิร์ฟเวอร์แล้ว", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "พิมพ์ไม่สำเร็จ: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                )
            ) {
                Text("Print QR")
            }

            // ปุ่มที่ 2: ยิงไป Zebra ZQ521 (ZPL:9100)
            Button(
                onClick = {
                    if (lastScanned.isEmpty()) return@Button
                    scope.launch {
                        try {
                            printZebraZQ521Qr(
                                ip = "xxx.xxx.xxx.xxx",
                                data = lastScanned,
                                labelWidthMm = 100,
                                labelLengthMm = 60,
                                qrSizeMm = 50,
                                copies = 1,
                                magnification = 6
                            )
                            Toast.makeText(context, "พิมพ์ไปที่ Zebra ZQ521 แล้ว", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "พิมพ์ไม่สำเร็จ: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                )
            )
            { Text("Print QR (Zebra)") }




            Button(
                onClick = {
                    if (lastScanned.isEmpty()) return@Button
                    scope.launch {
                        try {

                            printTscAlpha2rQrTspl(
                                ip = "xxx.xxx.xxx.xxx",
                                data = lastScanned,
                                port = 9100,
                                labelWidthMm = 58,
                                labelHeightMm = 60,
                                qrSizeMm = 50,
                                copies = 1

                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "พิมพ์ไปที่ TSC Alpha-2R แล้ว", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "พิมพ์ไม่สำเร็จ: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.DarkGray,
                    contentColor = Color.White
                )
            ) {
                Text("Print QR (TSC TSPL)")
            }
        }
    }
}




private const val PRINT_SERVER_IP = "xxx.xxx.xxx.xxx"
private const val LPR_QUEUE = "QUEUE_NAME"     // ← ชื่อแชร์ของเครื่องพิมพ์บนเซิร์ฟเวอร์
private const val ZEBRA_IP = "xxx.xxx.xxx.xxx"  // ← IP ของ ZQ521


private fun mmToDots(mm: Int, dpi: Int = 203): Int =
    ((mm / 25.4f) * dpi).roundToInt()

private fun zplSanitize(data: String): String {

    return data.replace("^", " ").replace("~", "-")
}

/**
 * พิมพ์ QR ไปที่ Zebra ZQ521 โดยตรงผ่าน RAW:9100 (ZPL)
 *
 * @param ip                 IP ของเครื่องพิมพ์ (หรือของ Zebra print server/bridge)
 * @param data               ข้อความที่ใส่ใน QR
 * @param port               ค่าเริ่มต้น 9100
 * @param labelWidthMm       ความกว้างสติ๊กเกอร์ (เช่น 100mm สำหรับ ZQ521)
 * @param labelLengthMm      ความยาวสติ๊กเกอร์ (เช่น 60mm)
 * @param qrSizeMm           ขนาด QR (เช่น 40–60mm)
 * @param copies             จำนวนสำเนา
 * @param magnification      ค่าซูมของ ^BQN (1–10) ปรับความละเอียดจุดของ QR (ปกติ 5–7)
 */
suspend fun printZebraZQ521Qr(
    ip: String,
    data: String,
    port: Int = 9100,
    labelWidthMm: Int = 100,
    labelLengthMm: Int = 60,
    qrSizeMm: Int = 50,
    copies: Int = 1,
    magnification: Int = 6
) = withContext(Dispatchers.IO) {
    val widthDots  = mmToDots(labelWidthMm)
    val lengthDots = mmToDots(labelLengthMm)
    val qrDots     = mmToDots(qrSizeMm)

    val x = max(0, (widthDots - qrDots) / 2)
    val y = max(0, (lengthDots - qrDots) / 2)

    val payload = zplSanitize(data)

    // ^BQN,2,<mag>  +  ^FDMA,<data>
    val zpl = """
        ^XA
        ^PW$widthDots
        ^LL$lengthDots
        ^LH0,0
        ^FO$x,$y
        ^BQN,2,$magnification
        ^FDMA,$payload^FS
        ^PQ$copies
        ^XZ
    """.trimIndent()

    Socket(ip, port).use { s ->
        s.getOutputStream().use { out ->
            out.write(zpl.toByteArray(Charsets.UTF_8))
            out.flush()
        }
    }
}

suspend fun printZebraRawZpl(ip: String, zpl: String, port: Int = 9100) =
    withContext(Dispatchers.IO) {
        Socket(ip, port).use { s ->
            s.getOutputStream().use { out ->
                out.write(zpl.toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }
    }


suspend fun printPdfToRicohRaw(
    ip: String,
    pdfFile: File,
    port: Int = 9100,
    usePjl: Boolean = true,
    jobName: String = "AndroidQR"
) = withContext(Dispatchers.IO) {
    require(pdfFile.exists()) { "PDF not found: ${pdfFile.absolutePath}" }

    val UEL = "\u001B%-12345X" // Universal Exit Language
    val preamble = if (usePjl) {
        // บางรุ่นต้องการบอกภาษาเป็น PDF ก่อน
        "$UEL@PJL JOB NAME=\"$jobName\"\r\n@PJL ENTER LANGUAGE=PDF\r\n"
    } else ""
    val postamble = if (usePjl) "$UEL@PJL EOJ\r\n$UEL" else ""

    Socket(ip, port).use { socket ->
        socket.getOutputStream().use { out ->
            if (preamble.isNotEmpty()) out.write(preamble.toByteArray(Charsets.US_ASCII))
            FileInputStream(pdfFile).use { it.copyTo(out) }
            if (postamble.isNotEmpty()) out.write(postamble.toByteArray(Charsets.US_ASCII))
            out.flush()
        }
    }
}

// Function to generate QR code bitmap
private fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bmp.setPixel(x, y, if (bitMatrix[x, y])  AndroidColor.BLACK else AndroidColor.TRANSPARENT)
        }
    }
    return bmp
}

private fun openPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Open PDF with"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            "ไม่พบแอปสำหรับเปิดไฟล์ PDF กรุณาติดตั้งแอปอ่าน PDF แล้วลองใหม่",
            Toast.LENGTH_LONG
        ).show()
    }
}


private fun printQrCode(context: Context, qrBitmap: Bitmap) {
    Toast.makeText(context, "Preparing QR Code for printing...", Toast.LENGTH_SHORT).show() // LENGTH_SHORT is usually enough for this

    val pdfFile = createPdfFromBitmap(context, qrBitmap, "qr_to_print.pdf")

    if (pdfFile != null && pdfFile.exists()) {
        android.util.Log.d("PrintQRCode", "PDF created at: ${pdfFile.absolutePath}")
        android.util.Log.d("PrintQRCode", "PDF size: ${pdfFile.length()} bytes")
        openPdf(context, pdfFile)
    } else {
        android.util.Log.e("PrintQRCode", "Failed to create PDF or file does not exist.")
        Toast.makeText(context, "Failed to create PDF for printing.", Toast.LENGTH_LONG).show()
    }

}

private fun createPdfFromBitmap(context: Context, bitmap: Bitmap, fileName: String): File? {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint()
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    pdfDocument.finishPage(page)


    val file = File(context.cacheDir, fileName)
    try {
        FileOutputStream(file).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return file
    } catch (e: IOException) {
        e.printStackTrace()
        pdfDocument.close()
    }
    return null
}


suspend fun printPdfViaLpr(
    ip: String,
    queue: String,
    pdfFile: File,
    port: Int = 515
) = withContext(Dispatchers.IO) {
    require(pdfFile.exists()) { "PDF not found: ${pdfFile.absolutePath}" }

    val host = "android"
    val jobId = Random.nextInt(100, 999)
    val dfName = "dfA$jobId$host"
    val cfName = "cfA$jobId$host"
    val fileDisplayName = pdfFile.name


    val control = buildString {
        appendLine("H$host")
        appendLine("Puser")
        appendLine("J$fileDisplayName")
        appendLine("N$fileDisplayName")
        appendLine("f$dfName")
        appendLine("U$dfName")
    }.toByteArray(Charsets.US_ASCII)

    Socket(ip, port).use { sock ->
        val input = sock.getInputStream()
        val output = sock.getOutputStream()

        fun ack() {
            val a = input.read()
            if (a != 0) throw IOException("LPR NACK: $a")
        }

        // 1) receive job
        output.write(0x02); output.write("$queue\n".toByteArray(Charsets.US_ASCII)); output.flush(); ack()
        // 2) control header
        output.write(0x02); output.write("${control.size} $cfName\n".toByteArray(Charsets.US_ASCII)); output.flush(); ack()
        // 3) control body + 0
        output.write(control); output.write(0x00); output.flush(); ack()
        // 4) data header
        val dataSize = pdfFile.length()
        output.write(0x03); output.write("$dataSize $dfName\n".toByteArray(Charsets.US_ASCII)); output.flush(); ack()
        // 5) data body + 0
        FileInputStream(pdfFile).use { it.copyTo(output) }
        output.write(0x00); output.flush(); ack()
    }
}

fun createA4PdfWithQr(context: Context, bitmap: Bitmap, fileName: String = "qr_a4.pdf"): File? {
    val pdf = PdfDocument()
    val mmToPt = 72f / 25.4f
    val widthPt = (210f * mmToPt).toInt()
    val heightPt = (297f * mmToPt).toInt()

    val pageInfo = PdfDocument.PageInfo.Builder(widthPt, heightPt, 1).create()
    val page = pdf.startPage(pageInfo)
    val c = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val qrSizePt = (70f * mmToPt)
    val left = (widthPt - qrSizePt) / 2f
    val top = (heightPt - qrSizePt) / 2f
    val dst = RectF(left, top, left + qrSizePt, top + qrSizePt)
    c.drawBitmap(bitmap, null, dst, paint)

    pdf.finishPage(page)

    val file = File(context.cacheDir, fileName)
    return try {
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close(); file
    } catch (e: IOException) {
        pdf.close(); null
    }
}

private fun sanitize(raw: String): String =
    raw.trim().trim('\n', '\r')


private fun processScan(context: Context, code: String) {
    if (code.isEmpty()) return
    Toast.makeText(context, "QR: $code", Toast.LENGTH_SHORT).show()
    beep()
}
private fun beep() {
    try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            .startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    } catch (_: Exception) { }
}



private fun clamp(v: Int, lo: Int, hi: Int) = max(lo, minOf(v, hi))

suspend fun printTscAlpha2rQrTspl(
    ip: String,
    data: String,
    port: Int = 9100,
    labelWidthMm: Int = 58,
    labelHeightMm: Int = 60,
    qrSizeMm: Int = 50,
    copies: Int = 1
) = withContext(kotlinx.coroutines.Dispatchers.IO) {

    val wDots = mmToDots(labelWidthMm)
    val hDots = mmToDots(labelHeightMm)
    val qrDots = mmToDots(qrSizeMm)


    val estimatedModules = if (data.length <= 20) 33
    else if (data.length <= 50) 41
    else 49


    var cell = (qrDots.toFloat() / estimatedModules).roundToInt()
    cell = clamp(cell, 3, 10)

    val x = max(0, (wDots - (cell * estimatedModules)) / 2)
    val y = max(0, (hDots - (cell * estimatedModules)) / 2)

    val tspl = """
        SIZE ${labelWidthMm} mm,${labelHeightMm} mm
        GAP 2 mm,0 mm
        DENSITY 8
        SPEED 4
        DIRECTION 1
        CLS
        QRCODE $x,$y,M,$cell,A,0,"$data"
        PRINT $copies,1
    """.trimIndent() + "\r\n"

    java.net.Socket(ip, port).use { s ->
        s.getOutputStream().use { out ->
            out.write(tspl.toByteArray(Charsets.US_ASCII))
            out.flush()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewScanScreen() {
    HelloApkTheme { ScanScreen() }
}