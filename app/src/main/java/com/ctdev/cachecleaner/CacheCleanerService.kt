package com.ctdev.cachecleaner

/**
 * Lớp này chạy trong một tiến trình RIÊNG với danh tính (UID) là shell (2000)
 * hoặc root (0), do Shizuku cấp khi bind UserService — không phải tiến trình
 * app bình thường. Vì vậy ProcessBuilder ở đây thực thi lệnh với quyền cao,
 * không cần Shizuku.newProcess (API đã bị khóa từ Shizuku-API 13.1.x).
 *
 * Bắt buộc phải có constructor không tham số.
 */
class CacheCleanerService : ICacheCleanerService.Stub() {

    override fun clearAllCache(): String {
        return try {
            // pm trim-caches dọn cache của MỌI app cho tới khi đạt dung lượng
            // trống chỉ định. Dùng số cực lớn để chắc chắn xóa hết toàn bộ.
            val process = ProcessBuilder("sh", "-c", "pm trim-caches 999999999999")
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()

            if (exitCode == 0) "OK" else "ERROR exit=$exitCode: $output"
        } catch (e: Exception) {
            "EXCEPTION: ${e.message}"
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}
