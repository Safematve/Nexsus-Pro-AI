package com.safematve.nexsuspro

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.util.Base64
import android.view.Gravity
import android.webkit.*
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val results = if (result.resultCode == Activity.RESULT_OK && data != null) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        } else null
        fileUploadCallback?.onReceiveValue(results)
        fileUploadCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#212121")
        window.navigationBarColor = Color.parseColor("#212121")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }

        val rootLayout = FrameLayout(this)
        webView = WebView(this)
        rootLayout.addView(webView)

        val pillBg = GradientDrawable().apply {
            setColor(Color.parseColor("#E62F2F2F"))
            cornerRadius = 50f
            setStroke(2, Color.parseColor("#33FFFFFF"))
        }

        val homePill = TextView(this).apply {
            text = "← Nexsus Pro"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(36, 18, 36, 18)
            background = pillBg
            visibility = android.view.View.GONE
            setOnClickListener {
                webView.loadUrl("file:///android_asset/index.html")
                visibility = android.view.View.GONE
            }
        }
        val params = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = 28
            marginEnd = 28
        }
        rootLayout.addView(homePill, params)
        setContentView(rootLayout)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = userAgentString.replace("; wv", "")
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                homePill.visibility = if (url != null && !url.startsWith("file:///android_asset/")) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback
                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
                fileChooserLauncher.launch(intent)
                return true
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                val popup = WebView(this@MainActivity).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = webView.settings.userAgentString
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true)
                val dialog = Dialog(this@MainActivity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                dialog.setContentView(popup)
                popup.webViewClient = WebViewClient()
                popup.webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) { dialog.dismiss() }
                }
                dialog.show()
                (resultMsg?.obj as? WebView.WebViewTransport)?.webView = popup
                resultMsg?.sendToTarget()
                return true
            }
        }

        webView.setDownloadListener { url, _, _, mimeType, _ ->
            try {
                if (url.startsWith("data:")) {
                    val base64Data = url.substringAfter(",")
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val ext = if (mimeType.contains("webm")) "webm" else "html"
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NexsusPro_${System.currentTimeMillis()}.$ext")
                    FileOutputStream(file).use { it.write(bytes) }
                    Toast.makeText(this, "Сохранено в Загрузки: ${file.name}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
