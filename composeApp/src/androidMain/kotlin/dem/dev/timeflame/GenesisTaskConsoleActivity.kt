package dem.dev.timeflame

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Optional standalone console. Works over USB with "adb reverse tcp:8081 tcp:8081". */
class GenesisTaskConsoleActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var token: EditText
    private lateinit var amount: EditText
    private lateinit var csv: EditText
    private lateinit var id: EditText
    private lateinit var result: TextView
    private lateinit var securityStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
        }
        fun add(view: android.view.View) = column.addView(
            view, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        add(TextView(this).apply { text = "GENESIS / Task Console"; textSize = 23f })
        add(TextView(this).apply { text = "Локальный сервис 127.0.0.1:8081 через USB/ADB reverse" })
        token = EditText(this).apply {
            hint = "Токен локального API"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        add(token)
        securityStatus = TextView(this).apply {
            text = "Level 2 / FINANCE: не проверено"
            textSize = 15f
        }
        add(securityStatus)
        add(Button(this).apply {
            text = "Состояние безопасности Level 2"
            setOnClickListener { checkSecurityLevel2() }
        })
        amount = EditText(this).apply {
            hint = "Количество тестовых заданий"
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("5")
        }
        add(amount)
        add(Button(this).apply {
            text = "Сгенерировать список"
            setOnClickListener {
                val count = amount.text.toString().toIntOrNull()
                if (count == null || count !in 1..40) {
                    result.text = "Введите количество от 1 до 40"
                } else send("/v1/tasks/generate", "POST", JSONObject().put("count", count).toString())
            }
        })
        csv = EditText(this).apply {
            hint = "CSV с тестовыми идентификаторами"
            minLines = 4
            setText("record_id,scenario\ndemo-001,positive\ndemo-002,negative\n")
        }
        add(csv)
        add(Button(this).apply {
            text = "Импортировать CSV"
            setOnClickListener {
                send("/v1/tasks/import", "POST",
                    JSONObject().put("csv_text", csv.text.toString()).toString())
            }
        })
        id = EditText(this).apply { hint = "dataset_id для загрузки списка" }
        add(id)
        add(Button(this).apply {
            text = "Загрузить список из БД"
            setOnClickListener {
                val key = id.text.toString().trim()
                if (!Regex("[0-9a-f]{32}").matches(key)) {
                    result.text = "Введите корректный dataset_id"
                } else send("/v1/tasks/$key", "GET", null)
            }
        })
        result = TextView(this).apply {
            text = "Ожидание задания"
            textSize = 14f
            setTextIsSelectable(true)
        }
        add(result)
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun checkSecurityLevel2() {
        val secret = token.text.toString()
        if (secret.isBlank()) {
            securityStatus.text = "Укажите API-токен"
            return
        }
        securityStatus.text = "Level 2 / FINANCE: проверка..."
        worker.execute {
            val message = try {
                val conn = URL("http://127.0.0.1:8081/v1/security/profile").openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 5000
                    conn.readTimeout = 10000
                    conn.setRequestProperty("X-Genesis-Token", secret)
                    val code = conn.responseCode
                    if (code != 200) {
                        "Level 2: HTTP $code"
                    } else {
                        val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val profile = json.optString("profile_id", "не выбран")
                        val deploy = json.optString("deployment_status", "unknown")
                        val allowed = json.optBoolean("execution_allowed", false)
                        "Level 2 / FINANCE\nПрофиль: $profile\n" +
                            "VPN → VM → Docker → VPN: $deploy\n" +
                            "Исполнение разрешено: $allowed"
                    }
                } finally { conn.disconnect() }
            } catch (ex: Exception) {
                "Level 2: ошибка связи с API"
            }
            ui.post { securityStatus.text = message }
        }
    }

    private fun send(route: String, method: String, body: String?) {
        val secret = token.text.toString()
        if (secret.isBlank()) { result.text = "Укажите API-токен"; return }
        result.text = "Запрос..."
        worker.execute {
            val response = try {
                val conn = URL("http://127.0.0.1:8081$route").openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 5000
                    conn.readTimeout = 10000
                    conn.requestMethod = method
                    conn.setRequestProperty("X-Genesis-Token", secret)
                    if (body != null) {
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    val formatted = try { JSONObject(raw).toString(2) } catch (_: Exception) { raw }
                    "HTTP $code\n$formatted"
                } finally { conn.disconnect() }
            } catch (e: Exception) {
                "Ошибка подключения: ${e.javaClass.simpleName}: ${e.message}"
            }
            ui.post {
                result.text = response
                // Dataset ID is a reference, not secret data; copy it to the fetch field.
                try {
                    val parsed = JSONObject(response.substringAfter(10.toChar()))
                    val datasetId = parsed.optString("dataset_id")
                    if (Regex("[a-f0-9]{32}").matches(datasetId)) id.setText(datasetId)
                } catch (_: Exception) { }
            }
        }
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }
}
