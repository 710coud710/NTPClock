# Quy trình Provisioning Device Owner (Phase 0 & Pilot)

> **Tham chiếu:** [spec-project.md](spec-project.md) (Mục 7)  
> **Application ID hiện tại:** `com.example.ntpclock`  
> **Component DPC:** `com.example.ntpclock/.admin.NtpDeviceAdminReceiver`

---

## 1. Điều kiện tiên quyết (Bắt buộc)

Android chỉ cho phép `dpm set-device-owner` khi thiết bị đáp ứng **đồng thời**:
- Không có tài khoản Google, Xiaomi Account, hoặc work profile nào đang đăng nhập trên máy (hoặc máy vừa **Factory Reset** và bỏ qua bước đăng nhập tài khoản trong Setup Wizard).
- Chưa có ứng dụng nào khác đang giữ vai trò Device Owner.
- Đã bật **Developer options** và **USB debugging**.

> **Lưu ý:** Nếu đổi `applicationId` trước khi build bản production, phải cập nhật lại đúng `<applicationId>/.admin.NtpDeviceAdminReceiver` trong lệnh ADB dưới đây.

---

## 2. Quy trình thực hiện bằng ADB (Thiết bị thử nghiệm)

```bash
# Bước 1: Kiểm tra thiết bị đã kết nối ADB
adb devices

# Bước 2: Cài đặt APK PoC
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Bước 3: Gán quyền Device Owner cho NtpDeviceAdminReceiver
adb shell dpm set-device-owner com.example.ntpclock/.admin.NtpDeviceAdminReceiver

# Bước 4: Xác minh trạng thái Device Owner ở cấp hệ thống
adb shell dumpsys device_policy | findstr /C:"Device Owner" /C:"com.example.ntpclock"
```

*(Trên Linux/macOS thay `findstr` bằng `grep -E "Device Owner|com.example.ntpclock"`.)*

---

## 3. Xử lý sự cố thường gặp trên Xiaomi / HyperOS

1. **`Not allowed to set the device owner because there are already several users on the device` hoặc `accounts on the device`**:
   - Vào **Settings → Accounts** xóa toàn bộ tài khoản (Google, Xiaomi, v.v.), xóa user phụ / Second Space nếu có; nếu vẫn lỗi thì **Factory Reset** và bỏ qua bước thêm tài khoản khi thiết lập ban đầu.
2. **HyperOS yêu cầu bật thêm quyền bảo mật USB**:
   - Kiểm tra mục **Install via USB** và **USB debugging (Security settings)** trong Developer Options nếu ADB bị chặn khi chạy `dpm`.
3. **Gỡ bỏ Device Owner trên máy thử nghiệm (nếu receiver đang dùng bản debug test)**:
   - Không dùng `adb uninstall` trực tiếp nếu device owner đang khóa; cần cấu hình hoặc factory reset tùy chính sách ROM. Chỉ giữ nguyên một `applicationId` ổn định cho các lần nâng cấp APK cùng chữ ký keystore.

---

## 4. Ghi chú cho triển khai hàng loạt (Sau Phase 0)

- Không triển khai QR provisioning offline hàng loạt trước khi hoàn tất kiểm chứng quy trình *factory-reset-to-ready* trên thiết bị thật.
- Lưu trữ **release keystore** tập trung; mọi bản cập nhật APK về sau phải ký cùng keystore để không mất trạng thái Device Owner.

