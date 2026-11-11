# Hồ sơ Tương thích Firmware — Redmi Pad 2 (Phase 0 Gate)

> **Tham chiếu:** [spec-project.md](spec-project.md) (Mục 6)  
> **Ứng dụng:** `com.example.ntpclock`  
> **Receiver DPC:** `com.example.ntpclock/.admin.NtpDeviceAdminReceiver`

---

## 1. Thông số thiết bị thử nghiệm (Thu thập qua ADB)

Chạy các lệnh sau khi kết nối Redmi Pad 2 qua USB Debugging:

```bash
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.fingerprint
adb shell getprop ro.product.device
adb shell getprop ro.build.version.security_patch
adb shell getprop ro.mi.os.version.name
```

| Trường | Giá trị ghi nhận |
| :--- | :--- |
| Model | *(điền sau khi đo trên máy thật)* |
| Android version (`ro.build.version.release`) | |
| API level (`ro.build.version.sdk`) | |
| HyperOS version (`ro.mi.os.version.name` / About tablet) | |
| Build fingerprint (`ro.build.fingerprint`) | |
| Product / Device (`ro.product.device`) | |
| Security patch (`ro.build.version.security_patch`) | |
| Serial thiết bị thử nghiệm | |

---

## 2. Ma trận kiểm chứng firmware (Phase 0)

| Hạng mục | Điều kiện đạt | Kết quả (`PASS`/`FAIL`) | Ghi chú |
| :--- | :--- | :--- | :--- |
| Cài đặt APK | `app-debug.apk` cài và mở được `PocScreen` | | |
| DPC Receiver | Hệ thống nhận diện `NtpDeviceAdminReceiver` | | |
| Device Owner Provisioning | `adb shell dpm set-device-owner` thành công | | |
| `isDeviceOwnerApp()` | UI hiển thị `Đạt (Device Owner)` | | |
| Tắt Automatic Date & Time | Nút **Tắt Auto Time** đưa `AUTO_TIME = 0` và giữ trạng thái | | |
| Truy vấn NTP nội bộ | **Test NTP (Chỉ đọc)** nhận phản hồi từ `192.168.4.2:123` | | |
| `DevicePolicyManager.setTime()` | Trả về `true` và không ném `SecurityException` | | |
| Thay đổi System Clock thực tế | Đồng hồ hệ thống đổi về giờ server (hậu kiểm đạt) | | |
| Timezone `Asia/Ho_Chi_Minh` | Nút **Đặt TZ UTC+7** đặt đúng múi giờ Việt Nam | | |
| Bền vững sau Reboot | Khởi động lại máy vẫn giữ `Device Owner` và `setTime()` chạy được | | |

---

## 3. Quyết định Go / No-Go

Cả 3 điều kiện cốt lõi phải đạt `PASS`:
1. Provision được ứng dụng thành **Device Owner**.
2. Tắt được `AUTO_TIME` và `DevicePolicyManager.setTime()` cập nhật được đồng hồ hệ thống thực tế.
3. Sau khi **reboot**, ứng dụng vẫn giữ vai trò **Device Owner** và tiếp tục đặt được giờ.

- **Kết luận:** `PENDING` *(chuyển sang `GO` hoặc `NO-GO` sau khi test trên Redmi Pad 2 thật)*
- **Người kiểm chứng / Ngày:** 

