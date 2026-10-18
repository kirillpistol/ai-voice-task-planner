package dem.dev.timeflame.feature.main.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executor
import kotlinx.coroutines.delay

/**
 * Local scan only. OCR never uploads images. The user must review and explicitly
 * confirm the extracted text before it enters the existing comment preview.
 */
@Composable
fun GenesisDocumentScanner(onDismiss: () -> Unit, onTextApproved: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember(context) { Executor { runnable -> ContextCompat.getMainExecutor(context).execute(runnable) } }
    var cameraGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var cameraReady by remember { mutableStateOf(false) }
    var capture by remember { mutableStateOf<ImageCapture?>(null) }
    var scannedText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var captured by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll(); recognizer.close() }
    }
    fun processImage(image: InputImage, bitmap: Bitmap? = null) {
        busy = true
        previewBitmap = bitmap
        recognizer.process(image)
            .addOnSuccessListener { result ->
                scannedText = result.text
                if (result.text.isBlank()) error = "Текст не распознан. Попробуйте другое освещение или выберите фото."
                busy = false
            }
            .addOnFailureListener {
                error = "Не удалось распознать документ"
                busy = false
            }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        cameraGranted = it
        if (!it) error = "Для камеры нужно разрешение Android. Галерея доступна отдельно."
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        captured = true
                        processImage(InputImage.fromBitmap(bmp, 0), bmp)
                    } else error = "Не удалось прочитать изображение"
                }
            } catch (_: Exception) { error = "Не удалось открыть выбранный файл" }
        }
    }
    fun takePhoto() {
        val camera = capture ?: return
        if (busy || captured) return
        busy = true
        val file = File.createTempFile("genesis-scan-", ".jpg", context.cacheDir)
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        camera.takePicture(output, executor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                captured = true
                try {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) processImage(InputImage.fromFilePath(context, Uri.fromFile(file)), bmp)
                    else { busy = false; error = "Не удалось открыть снимок" }
                } catch (_: Exception) { busy = false; error = "Не удалось прочитать снимок" }
                finally { file.delete() }
            }
            override fun onError(exception: ImageCaptureException) {
                busy = false
                error = "Снимок не получен. Попробуйте ещё раз."
                file.delete()
            }
        })
    }
    val lineTransition = rememberInfiniteTransition(label = "scanner")
    val linePosition by lineTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan-line"
    )
    // First capture only after the preview is running and has had time to settle.
    // This is a timed MVP capture, NOT verified focus/document detection.
    LaunchedEffect(cameraReady, cameraGranted, captured, busy) {
        if (cameraReady && cameraGranted && !captured && !busy) {
            delay(2500)
            if (cameraReady && !captured && !busy) takePhoto()
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().imePadding(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF30343A),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (cameraGranted && !captured) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1.15f).clip(RoundedCornerShape(12.dp)).background(Color.Black)) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    val future = ProcessCameraProvider.getInstance(ctx)
                                    future.addListener({
                                        try {
                                            val provider = future.get()
                                            val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                                            val photo = ImageCapture.Builder().build()
                                            provider.unbindAll()
                                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, photo)
                                            capture = photo
                                            cameraProvider = provider
                                            cameraReady = true
                                        } catch (_: Exception) { error = "Камера недоступна" }
                                    }, executor)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            Box(
                                Modifier.fillMaxWidth().height(3.dp)
                                    .offset(y = maxHeight * linePosition)
                                    .background(Color(0xFF42E9EF))
                            )
                        }
                    }
                } else if (!cameraGranted) {
                    Text("Разрешите доступ к камере или выберите изображение из галереи.", color = Color.White)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Разрешить камеру")
                    }
                }
                if (captured && previewBitmap != null) {
                    Image(bitmap = previewBitmap!!.asImageBitmap(), contentDescription = "Снимок документа",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp), contentScale = ContentScale.Fit)
                }
                if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (scannedText.isNotBlank()) {
                    OutlinedTextField(
                        value = scannedText, onValueChange = { scannedText = it },
                        label = { Text("Проверьте и исправьте распознанный текст") },
                        modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black, unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                            focusedLabelColor = Color.White, unfocusedLabelColor = Color.White,
                            cursorColor = Color.Black, focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color.Black)) { Text("Галерея") }
                    if (cameraGranted) {
                        OutlinedButton(onClick = { captured = false; scannedText = ""; previewBitmap = null; error = null },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                            Text("Повторить")
                        }
                    }
                }
                Button(
                    onClick = { onTextApproved(scannedText.trim()) },
                    enabled = scannedText.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF8B8D90), disabledContentColor = Color.White)
                ) { Text("✓ Добавить информацию") }
                TextButton(onClick = onDismiss) { Text("Закрыть", color = Color.White) }
            }
        }
    }
}
