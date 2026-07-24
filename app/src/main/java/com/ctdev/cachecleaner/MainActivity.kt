package com.ctdev.cachecleaner

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var clearButton: Button
    private lateinit var openShizukuButton: Button
    private lateinit var userServiceArgs: Shizuku.UserServiceArgs

    private var cacheService: ICacheCleanerService? = null

    companion object {
        private const val REQUEST_CODE = 1001
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
    }

    private val permissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    updateStatus("Đã cấp quyền Shizuku ✅\nSẵn sàng xóa cache toàn bộ app.")
                    clearButton.isEnabled = true
                } else {
                    updateStatus("Bạn đã từ chối quyền Shizuku.\nKhông thể xóa cache nếu không cấp quyền.")
                    clearButton.isEnabled = false
                }
            }
        }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { checkPermissionAndUpdateUi() }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            updateStatus("Shizuku đã ngừng chạy.\nHãy mở app Shizuku và bấm Start lại.")
            clearButton.isEnabled = false
        }
    }

    // Kết nối tới CacheCleanerService chạy dưới quyền shell/root do Shizuku cấp
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                cacheService = ICacheCleanerService.Stub.asInterface(binder)
                runClearCache()
            } else {
                runOnUiThread {
                    clearButton.isEnabled = true
                    updateStatus("❌ Không kết nối được service dọn cache.")
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            cacheService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        clearButton = findViewById(R.id.clearButton)
        openShizukuButton = findViewById(R.id.openShizukuButton)

        userServiceArgs = Shizuku.UserServiceArgs(
            ComponentName(packageName, CacheCleanerService::class.java.name)
        )
            .daemon(false)
            .processNameSuffix("cache_service")
            .debuggable(false)
            .version(1)

        clearButton.isEnabled = false
        clearButton.setOnClickListener { clearAllCache() }
        openShizukuButton.setOnClickListener { openOrInstallShizuku() }

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndUpdateUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        if (::userServiceArgs.isInitialized) {
            try {
                Shizuku.unbindUserService(userServiceArgs, serviceConnection, true)
            } catch (_: Exception) {
                // Shizuku có thể đã ngừng chạy, bỏ qua
            }
        }
    }

    private fun checkPermissionAndUpdateUi() {
        if (!Shizuku.pingBinder()) {
            updateStatus(
                "Chưa kết nối được Shizuku.\n" +
                        "Hãy cài & mở app Shizuku, bấm Start, rồi quay lại đây."
            )
            clearButton.isEnabled = false
            return
        }

        if (Shizuku.isPreV11()) {
            updateStatus("Phiên bản Shizuku quá cũ, hãy cập nhật app Shizuku rồi thử lại.")
            clearButton.isEnabled = false
            return
        }

        when {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> {
                updateStatus("Đã sẵn sàng ✅\nBấm nút bên dưới để xóa cache toàn bộ app.")
                clearButton.isEnabled = true
            }
            Shizuku.shouldShowRequestPermissionRationale() -> {
                updateStatus(
                    "Bạn đã từ chối quyền trước đó.\n" +
                            "Vào app Shizuku > mục quyền, cấp lại quyền cho app này."
                )
                clearButton.isEnabled = false
            }
            else -> {
                updateStatus("Đang xin quyền Shizuku...")
                Shizuku.requestPermission(REQUEST_CODE)
            }
        }
    }

    private fun updateStatus(text: String) {
        statusText.text = text
    }

    /** Bước 1: khởi động/kết nối tới service chạy với quyền shell/root. */
    private fun clearAllCache() {
        clearButton.isEnabled = false
        updateStatus("Đang khởi tạo service quyền cao...")
        try {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
        } catch (e: Exception) {
            clearButton.isEnabled = true
            updateStatus("❌ Lỗi khi khởi tạo service: ${e.message}")
        }
    }

    /** Bước 2: service đã kết nối, gọi lệnh dọn cache trên luồng nền. */
    private fun runClearCache() {
        updateStatus("Đang xóa cache toàn bộ app, vui lòng đợi...")
        Thread {
            val result = try {
                cacheService?.clearAllCache() ?: "ERROR: service null"
            } catch (e: Exception) {
                "EXCEPTION: ${e.message}"
            }

            runOnUiThread {
                clearButton.isEnabled = true
                if (result == "OK") {
                    updateStatus("✅ Đã xóa xong cache của toàn bộ ứng dụng!")
                    Toast.makeText(this, "Đã xóa cache thành công", Toast.LENGTH_SHORT).show()
                } else {
                    updateStatus("⚠️ Có lỗi khi xóa cache:\n$result")
                }
            }
        }.start()
    }

    private fun openOrInstallShizuku() {
        val launchIntent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL)))
        }
    }
}
