package com.ctdev.cachecleaner;

interface ICacheCleanerService {
    // Chạy lệnh dọn cache, trả về "OK" nếu thành công hoặc mô tả lỗi
    String clearAllCache() = 1;

    // Bắt buộc phải có: Shizuku gọi hàm này để yêu cầu service tự thoát
    void destroy() = 16777114;
}
