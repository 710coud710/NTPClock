# Kế hoạch Kiểm thử (Test Plan) — NTPClock

> **Tham chiếu:** [spec-project.md](spec-project.md) (Mục 12)

---

## 1. Kiểm thử Unit Test (JVM)

Các bài test đã cài đặt trong `app/src/test/java/com/example/ntpclock/`:

| Test Suite | Phạm vi kiểm thử |
| :--- | :--- |
| `NtpTimestampTest` | Chuyển đổi hai chiều NTP 64-bit (1900) ↔ Unix millis (1970); xử lý era 1 rollover sau năm 2036. |
| `NtpPacketTest` | Đóng gói client request 48 byte (`0x23`), giải mã response, kiểm tra từ chối sai `originateTimestamp`, `stratum == 0`, `LI == 3`. |
| `NtpSyncCoordinatorTest` | Kiểm thử 5 trạng thái (`NTP_SUCCESS`, `CLOCK_SYNC_SUCCESS`, `CLOCK_SYNC_DENIED`, `NTP_FAILED`, `CLOCK_SYNC_VERIFY_FAILED`) và cộng trễ monotonic từ `T4` tới `setTime()`. |

---

## 2. Kiểm thử Phase 0 PoC trên Redmi Pad 2 thật

| ID | Kịch bản | Thao tác trên `PocScreen` | Kết quả mong đợi |
| :--- | :--- | :--- | :--- |
| `P0-1` | Kiểm tra trước khi provision | Mở app khi chưa chạy `dpm set-device-owner` | `Device Owner = Chưa cấp quyền`; bấm **Đồng bộ ngay** trả `CLOCK_SYNC_DENIED`. |
| `P0-2` | Provision Device Owner | Chạy `adb shell dpm set-device-owner ...` rồi bấm **Làm mới** | `Device Owner = Đạt (Device Owner)`. |
| `P0-3` | Tắt Automatic Date & Time | Bấm **Tắt Auto Time** | `Automatic Date & Time = Đã tắt (AUTO_TIME=0)`. |
| `P0-4` | Test kết nối NTP nội bộ | Kết nối Wi-Fi công ty, bấm **Test NTP (Chỉ đọc)** tới `192.168.4.2:123` | Trạng thái `NTP_SUCCESS`, hiển thị `Offset`, `Delay(RTT)`, `Stratum` (1..15); giờ máy chưa bị đổi. |
| `P0-5` | Đồng bộ & đặt giờ thực tế | Chỉnh giờ máy lệch 5 phút (hoặc giữ nguyên), bấm **Đồng bộ ngay** | Trạng thái `CLOCK_SYNC_SUCCESS`, đồng hồ hệ thống nhảy về giờ chuẩn, sai số hậu kiểm ≤ ±2000 ms. |
| `P0-6` | Đặt Timezone Việt Nam | Bấm **Đặt TZ UTC+7** | Timezone hiển thị `Asia/Ho_Chi_Minh`, giờ địa phương đúng UTC+7. |
| `P0-7` | Bền vững sau Reboot | Khởi động lại Redmi Pad 2, mở app | Vẫn giữ `Device Owner = Đạt`, nút **Đồng bộ ngay** tiếp tục trả `CLOCK_SYNC_SUCCESS`. |

---

## 3. Kiểm thử mở rộng (Phase 1–4 sau khi đạt Phase 0 Gate)

Bám sát bộ kiểm thử `T01`–`T15` trong `spec-project.md` Mục 12.1:
- `T01`–`T04`: Multi-sample 5 gói NTP, loại bỏ packet không hợp lệ / unsynchronized / sai originate, chọn mẫu RTT thấp hoặc median.
- `T05`–`T10`: Quyền Device Owner, `AUTO_TIME`, đặt giờ lệch 5 phút, timezone UTC+7, reboot giữ nguyên chính sách.
- `T11`–`T12`: Mất Wi-Fi không dùng thời gian cache để chỉnh; kết nối lại đồng bộ bình thường.
- `T13`–`T15`: `WorkManager` định kỳ 60 phút, nhiều tablet đồng bộ cùng server `192.168.4.2`, kiểm tra lại sau OTA firmware.

