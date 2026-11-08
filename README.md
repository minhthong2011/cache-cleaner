# Dọn Cache Toàn Bộ App (Shizuku) — 1 chạm, không root

App Android dùng framework **Shizuku** để xóa cache của **tất cả** app trên máy
chỉ với 1 nút bấm, không cần root máy.

## Vì sao cần Shizuku?

Android sandbox mỗi app, một app thường không có quyền đụng vào dữ liệu của
app khác. Shizuku cấp cho app quyền "shell" (giống ADB) một cách hợp pháp,
không cần mở khóa bootloader hay root máy. Đây chính là cách các app dọn
dẹp nổi tiếng như SD Maid SE làm điều này.

## Bước 1 — Cài & bật Shizuku trên điện thoại

1. Cài app **Shizuku**: <https://shizuku.rikka.app/download/> (hoặc tìm
   "Shizuku" trên Play Store / GitHub Releases).
2. Mở app Shizuku, chọn cách khởi động:
   - **Android 11 trở lên (không root):** Cài đặt → Tùy chọn nhà phát triển
     → bật **Gỡ lỗi không dây (Wireless debugging)** → vào lại Shizuku,
     chọn "Ghép nối qua gỡ lỗi không dây theo cặp" theo hướng dẫn trong app.
     Làm 1 lần, nhưng cần mở lại Shizuku (bấm Start) sau mỗi lần khởi động
     lại máy vì máy chưa root.
   - **Máy đã root:** Shizuku có thể tự khởi động bằng quyền root, không cần
     làm lại mỗi lần khởi động máy.
3. Thấy trạng thái "Đang chạy" màu xanh trong app Shizuku là xong.

## Bước 2 — Build app này bằng Android Studio

1. Mở **Android Studio** → File → Open → chọn thư mục `ShizukuCacheCleaner`.
2. Android Studio sẽ tự tải Gradle wrapper và đồng bộ dự án (cần mạng).
3. Cắm điện thoại (bật gỡ lỗi USB) hoặc dùng máy ảo, bấm **Run ▶**.

## Bước 3 — Dùng app

1. Mở app "Dọn Cache (Shizuku)" vừa cài.
2. Lần đầu app sẽ hiện popup xin quyền từ Shizuku — bấm **Cho phép**.
3. Bấm **XÓA TOÀN BỘ CACHE** — app chạy lệnh hệ thống `pm trim-caches` để
   dọn cache mọi app cùng lúc.
4. Nếu thấy "Chưa kết nối được Shizuku", bấm nút **Mở app Shizuku** ở dưới
   để bật lại.

## Giới hạn cần biết

- Không root: phải mở lại Shizuku sau mỗi lần khởi động máy (giới hạn củaa)
  chính Shizuku/Android, không phải của app này).
- `pm trim-caches` chỉ xóa **cache**, không đụng đến dữ liệu đăng nhập hay
  dữ liệu app (an toàn, giống nút "Xóa bộ nhớ đệm" trong Cài đặt).
- API `Shizuku.newProcess` được Shizuku đánh dấu sẽ thay bằng `UserService`
  trong tương lai xa; nếu sau này Shizuku gỡ hẳn API này, phần code chạy
  lệnh cần chuyển sang UserService (xem README của Shizuku-API).
