package com.ctdev.cachecleaner

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var clearButton: Button
    private lateinit var openShizukuButton: Button

    companion object {
        private const val REQUEST_CODE = 1001

        // Shizuku (app quản lý) trên Play Store / GitHub
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
    }

    // Được gọi khi người dùng chấp nhận / từ chối popup xin quyền Shizuku
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

    // Được gọi khi app kết nối được với service Shizuku
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        runOnUiThread { checkPermissionAndUpdateUi() }
    }

    // Được gọi khi Shizuku bị tắt / mất kết nối
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        runOnUiThread {
            updateStatus("Shizuku đã ngừng chạy.\nHãy mở app Shizuku và bấm Start lại.")
            clearButton.isEnabled = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        clearButton = findViewById(R.id.clearButton)
        openShizukuButton = findViewById(R.id.openShizukuButton)

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
    }

    /** Kiểm tra trạng thái Shizuku + quyền, cập nhật giao diện tương ứng. */
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

    /**
     * Xóa cache của TOÀN BỘ app trên máy bằng lệnh hệ thống `pm trim-caches`,
     * chạy với quyền shell (ADB) do Shizuku cấp — không cần root.
     *
     * `pm trim-caches <bytes>` dọn cache mọi app cho tới khi giải phóng đủ
     * dung lượng chỉ định. Truyền một số cực lớn để chắc chắn xóa hết toàn bộ.
     */
    private fun clearAllCache() {
        clearButton.isEnabled = false
        updateStatus("Đang xóa cache toàn bộ app, vui lòng đợi...")

        Thread {
            try {
                val process = Shizuku.newProcess(
                    arrayOf("sh", "-c", "pm trim-caches 999999999999"),
                    null,
                    null
                )
                val exitCode = process.waitFor()
                val errorOutput = process.errorStream.bufferedReader().readText().trim()

                runOnUiThread {
                    clearButton.isEnabled = true
                    if (exitCode == 0) {
                        updateStatus("✅ Đã xóa xong cache của toàn bộ ứng dụng!")
                        Toast.makeText(this, "Đã xóa cache thành công", Toast.LENGTH_SHORT).show()
                    } else {
                        updateStatus("⚠️ Lệnh chạy xong nhưng có cảnh báo (exit $exitCode):\n$errorOutput")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    clearButton.isEnabled = true
                    updateStatus("❌ Lỗi khi xóa cache: ${e.message}")
                }
            }
        }.start()
    }

    /** Mở app Shizuku nếu đã cài, ngược lại mở trang tải Shizuku. */
    private fun openOrInstallShizuku() {
        val launchIntent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL)))
        }
    }
}
