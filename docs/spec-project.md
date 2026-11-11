# NTP Clock Client for Redmi Pad 2

> **Tài liệu:** `spec-project.md`\
> **Phiên bản:** 1.0\
> **Ngày:** 11/11/2026\
> **Trạng thái:** Technical specification / implementation plan\
> **Thiết bị mục tiêu:** Xiaomi Redmi Pad 2\
> **NTP server nội bộ:** `192.168.4.2:123/UDP`

------------------------------------------------------------------------

## 1. Mục tiêu dự án

Xây dựng ứng dụng Android dạng APK để đồng bộ **đồng hồ hệ thống thực tế
(System Clock)** của các máy tính bảng Redmi Pad 2 với NTP Server
Windows nội bộ của công ty.

Yêu cầu nghiệp vụ:

-   Thiết bị không cần Internet; chỉ cần kết nối được mạng LAN/Wi-Fi nội
    bộ công ty.
-   Tắt chức năng tự động lấy ngày giờ từ mạng của Android.
-   Người dùng có thể bấm nút **Đồng bộ ngay** để lấy giờ từ máy chủ.
-   Ứng dụng có thể tự đồng bộ định kỳ mỗi 60 phút.
-   Có thể đồng bộ lại sau khi thiết bị khởi động.
-   Thời gian được đặt ở cấp hệ điều hành, không chỉ hiển thị giờ NTP
    trong giao diện ứng dụng.
-   Triển khai cho nhiều thiết bị; mỗi thiết bị kết nối trực tiếp tới
    cùng NTP Server.
-   Không xây dựng backend/API server riêng và không yêu cầu Internet
    trong quá trình vận hành.

### 1.1. Ngoài phạm vi

-   Không xây dựng máy chủ quản lý thiết bị (EMM) riêng ở giai đoạn đầu.
-   Không thay thế hoặc cấu hình lại Windows Time Service nếu máy chủ
    hiện tại đã phục vụ NTP ổn định.
-   Không yêu cầu root hoặc flash ROM ở phương án ưu tiên.
-   Không đảm bảo đồng bộ khi thiết bị mất kết nối LAN/Wi-Fi tới máy
    chủ.
-   Không dùng thời gian lấy từ một thiết bị Android khác làm nguồn
    chuẩn.

------------------------------------------------------------------------

## 2. Kiến trúc tổng thể

``` text
                   MẠNG LAN / WI-FI CÔNG TY
                              |
                              |
             +----------------+----------------+
             |                                 |
   +----------------------+          +----------------------+
   | Windows NTP Server  |          | Windows NTP Server   |
   | 192.168.4.2         |          | W32Time              |
   | UDP 123              |          | Nguồn thời gian chuẩn|
   +----------------------+          +----------------------+
             |
             | NTP v4 Request / Response (UDP 123)
             |
   +---------+----------------------------------------------+
   |                     Android Tablets                    |
   |                                                        |
   |  +-------------------+       +----------------------+  |
   |  | Redmi Pad 2 #01   |       | Redmi Pad 2 #02      |  |
   |  | NTP Client APK    |       | NTP Client APK       |  |
   |  +---------+---------+       +----------+-----------+  |
   |            |                            |              |
   |  +---------v---------+       +----------v-----------+  |
   |  | Time Calculator   |       | Time Calculator      |  |
   |  +---------+---------+       +----------+-----------+  |
   |            |                            |              |
   |  +---------v---------+       +----------v-----------+  |
   |  | DevicePolicyMgr   |       | DevicePolicyMgr      |  |
   |  | Device Owner      |       | Device Owner         |  |
   |  +---------+---------+       +----------+-----------+  |
   |            |                            |              |
   |  +---------v---------+       +----------v-----------+  |
   |  | Android System    |       | Android System       |  |
   |  | Clock             |       | Clock                |  |
   |  +-------------------+       +----------------------+  |
   +--------------------------------------------------------+
```

Mỗi máy tính bảng là một NTP Client độc lập. Không có luồng dữ liệu thời
gian đi qua backend trung gian.

### 2.1. Thành phần phía máy chủ

Tận dụng Windows Server hiện tại:

  Thuộc tính        Giá trị
  ----------------- -------------------------------------------
  Địa chỉ           `192.168.4.2`
  Dịch vụ           Windows Time Service (`W32Time`)
  Giao thức         NTP
  Port              UDP 123
  Vai trò           Cung cấp thời gian chuẩn cho các thiết bị
  Backend bổ sung   Không cần

Kết quả đã kiểm tra từ máy Windows cá nhân:

``` text
w32tm /stripchart /computer:192.168.4.2 /samples:5 /dataonly

Tracking 192.168.4.2 [192.168.4.2:123].
Collecting 5 samples.
13:40:10, +00.0075657s
13:40:12, +00.0075927s
13:40:14, +00.0076150s
13:40:16, +00.0071633s
13:40:18, +00.0076381s
```

Kết quả này chứng minh máy Windows kiểm tra đã nhận được phản hồi NTP từ
`192.168.4.2`. Độ lệch được công cụ báo cáo xấp xỉ 7,6 ms tại thời điểm
thử nghiệm. Đây chưa phải là kiểm thử từ Redmi Pad 2; cần kiểm tra UDP
123 trực tiếp từ mạng Wi-Fi mà tablet đang sử dụng.

------------------------------------------------------------------------

## 3. Công nghệ sử dụng

### 3.1. Android application

  -----------------------------------------------------------------------
  Công nghệ               Vai trò                 Lý do
  ----------------------- ----------------------- -----------------------
  Kotlin                  Ngôn ngữ chính          Ngôn ngữ ưu tiên cho
                                                  Android native

  Android Studio          IDE                     Build, debug, ký APK

  Jetpack Compose         UI                      Xây dựng giao diện hiện
                                                  đại

  MVVM                    Kiến trúc UI            Tách UI, state và logic

  Kotlin Coroutines       Xử lý bất đồng bộ       Không chạy UDP/network
                                                  trên main thread

  Java NIO DatagramSocket UDP socket              Gửi/nhận gói NTP
  hoặc DatagramSocket                             

  DevicePolicyManager     Quản trị thiết bị       API hệ thống để Device
                                                  Owner đặt giờ

  DeviceAdminReceiver     DPC receiver            Đăng ký ứng dụng quản
                                                  trị thiết bị

  WorkManager             Lịch đồng bộ nền        Lập lịch định kỳ có lưu
                                                  bền qua lần khởi động

  Room                    Lịch sử đồng bộ         Lưu log cục bộ

  DataStore               Cấu hình                Lưu NTP host, chu kỳ,
                                                  ngưỡng sai lệch

  Android Keystore (nếu   Bảo vệ bí mật           Chỉ dùng nếu phát sinh
  cần)                                            thông tin xác thực
  -----------------------------------------------------------------------

### 3.2. Cấu hình build đề xuất

-   Ngôn ngữ: Kotlin.
-   UI: Jetpack Compose.
-   `minSdk`: API 28 (Android 9), vì `DevicePolicyManager.setTime()`
    được thêm từ API 28.
-   `compileSdk` và `targetSdk`: chọn phiên bản SDK ổn định được Android
    Studio hỗ trợ tại thời điểm bắt đầu dự án; không cố định theo một số
    phiên bản plugin cũ trong tài liệu này.
-   Build output: APK ký bằng release keystore do công ty quản lý.
-   Không yêu cầu thư viện NTP bên thứ ba bắt buộc; có thể tự triển khai
    phần NTP packet để kiểm soát validation và timestamp.

**Lưu ý:** `minSdk 28` chỉ thể hiện API nền tảng tối thiểu có phương
thức `setTime()`. Nó không đảm bảo mọi firmware Android/HyperOS đều cho
phép provisioning Device Owner hoặc thực thi chính sách theo cùng một
cách.

------------------------------------------------------------------------

## 4. Kỹ thuật lấy thời gian bằng NTP

### 4.1. Giao thức

Sử dụng Network Time Protocol v4 (NTPv4), RFC 5905, qua UDP port 123.

Cấu hình mặc định:

``` yaml
ntp:
  host: "192.168.4.2"
  port: 123
  version: 4
  timeout_ms: 2500
  samples_per_sync: 5
  sample_interval_ms: 250
  timezone: "Asia/Ho_Chi_Minh"
```

Các giá trị trên là cấu hình khởi đầu; cần điều chỉnh sau kiểm thử mạng
nội bộ.

### 4.2. Trao đổi timestamp

NTP dùng bốn timestamp:

-   `T1`: thời điểm client gửi request.
-   `T2`: thời điểm server nhận request.
-   `T3`: thời điểm server gửi response.
-   `T4`: thời điểm client nhận response.

Công thức ước tính:

``` text
offset = ((T2 - T1) + (T3 - T4)) / 2
delay  = (T4 - T1) - (T3 - T2)

estimated_server_time_at_T4 = T4 + offset
```

`offset` là độ lệch ước tính giữa đồng hồ server và client. Quy ước dấu
trong phần mềm phải được thống nhất: offset dương nghĩa là server đi
trước client.

Không lấy timestamp từ thời điểm UI cập nhật. Việc ghi nhận thời gian
gửi/nhận phải thực hiện sát thời điểm thao tác socket.

### 4.3. Đo thời gian đơn điệu và thời gian hệ thống

Việc đồng bộ có thể làm thay đổi wall clock. Do đó:

-   Dùng `SystemClock.elapsedRealtimeNanos()` để đo khoảng thời gian đơn
    điệu, không bị ảnh hưởng khi đồng hồ hệ thống nhảy.
-   Ghi nhận wall clock riêng để ánh xạ thời gian NTP.
-   Không dùng phép trừ hai wall-clock timestamp làm phép đo RTT nếu
    đồng hồ có thể bị chỉnh trong lúc request đang chạy.
-   Sau khi đặt giờ, không tái sử dụng phép đo cũ như một phép đo mới.

### 4.4. Validation gói NTP

Chỉ chấp nhận phản hồi thỏa mãn các điều kiện:

-   NTP version hợp lệ.
-   Mode phản hồi server hợp lệ.
-   Leap Indicator không báo trạng thái unsynchronized.
-   Stratum nằm trong phạm vi hợp lệ (1--15).
-   Originate Timestamp khớp Transmit Timestamp của request tương ứng.
-   Timestamp server không bằng 0 và không ở trạng thái chưa đồng bộ.
-   RTT không vượt quá ngưỡng cấu hình.
-   Offset giữa các mẫu không phân tán bất thường.
-   Không nhận phản hồi đến từ endpoint ngoài endpoint đã cấu hình nếu
    không được cho phép.

Cần xử lý NTP era rollover và chuyển đổi timestamp NTP (epoch 1900) sang
Unix epoch (epoch 1970) đúng cách.

### 4.5. Chọn kết quả

-   Gửi 5 mẫu, cách nhau khoảng 250 ms.
-   Loại mẫu timeout hoặc không hợp lệ.
-   Xếp hạng theo RTT và chất lượng mẫu.
-   Ưu tiên mẫu có RTT thấp, không có dấu hiệu bất thường.
-   Có thể dùng median của các offset hợp lệ để giảm ảnh hưởng outlier.
-   Nếu số mẫu hợp lệ ít hơn ngưỡng tối thiểu, hủy lần đồng bộ và báo
    lỗi.

Không coi `w32tm /stripchart` là phép đo chính xác tuyệt đối cho
Android; ứng dụng cần tự đo trên từng tablet.

------------------------------------------------------------------------

## 5. Cách đặt giờ hệ thống Android

### 5.1. API sử dụng

API chính:

``` text
android.app.admin.DevicePolicyManager.setTime(
    admin: ComponentName,
    millis: Long
): Boolean
```

-   Có từ API 28.
-   `millis` là Unix epoch milliseconds.
-   Theo Android API reference, phương thức dành cho Device Owner hoặc
    một số loại Profile Owner được hỗ trợ.
-   Chỉ có hiệu lực khi `Settings.Global.AUTO_TIME` đang bằng 0.
-   Trả về `true` nếu thao tác đặt giờ thành công, `false` nếu không
    thành công.
-   Có thể ném `SecurityException` nếu ứng dụng không có vai trò quản
    trị phù hợp.

Không dùng `AlarmManager.setTime()` như API ứng dụng thông thường để đặt
giờ. Không dựa vào quyền `SET_TIME` thông thường trong manifest để vượt
qua giới hạn hệ thống.

### 5.2. Device Owner / DPC

Ứng dụng cần có thành phần Device Policy Controller (DPC):

-   Một lớp kế thừa `DeviceAdminReceiver`.
-   Khai báo receiver và device-admin metadata trong manifest.
-   Khai báo policy XML phù hợp.
-   Kiểm tra trạng thái
    `DevicePolicyManager.isDeviceOwnerApp(packageName)`.
-   Chỉ cho phép chức năng đặt giờ khi ứng dụng thực sự là Device Owner.
-   Xử lý `SecurityException`, kết quả `false` và trạng thái
    provisioning chưa hoàn tất.

**Device Admin thông thường không đồng nghĩa với Device Owner.** Người
dùng bật quyền quản trị ứng dụng trong Settings không đủ để gọi
`setTime()`.

### 5.3. Tắt Automatic Date & Time

`setTime()` chỉ có hiệu lực khi automatic time đang tắt.

Trên Android API 30 trở lên, DPC có API quản lý chế độ tự động thời gian
như `setAutoTimeEnabled(admin, false)` (kiểm tra API cụ thể theo compile
SDK). Sau đó cần xác minh trạng thái thực tế của thiết bị.

Trên API thấp hơn, không được giả định có cùng API. Cần cấu hình ban đầu
theo cách được firmware hỗ trợ và kiểm tra `AUTO_TIME` thực tế.

Ứng dụng nên có chế độ kiểm tra trước khi đồng bộ:

``` text
1. Kiểm tra ứng dụng có Device Owner hay không.
2. Kiểm tra automatic time đang tắt.
3. Kiểm tra có thể gọi DPM setTime hay không.
4. Nếu không đáp ứng, dừng và hiển thị hướng dẫn.
```

Không nên tự động thay đổi policy thời gian của thiết bị trong lần chạy
đầu nếu chưa được quản trị viên cho phép.

### 5.4. Quy trình đặt giờ

``` text
User / WorkManager
        |
        v
NtpSyncCoordinator
        |
        v
NtpClient.query(server)
        |
        v
Validate NTP response
        |
        v
Calculate offset / estimated time
        |
        v
Check Device Owner + AUTO_TIME disabled
        |
        v
DevicePolicyManager.setTime(admin, epochMillis)
        |
        v
Read System.currentTimeMillis()
        |
        v
Verify result and persist SyncHistory
```

### 5.5. Chọn timestamp để đặt

Dùng thời gian server đã ước tính tại thời điểm client nhận response
(`T4 + offset`), sau đó cộng thêm thời gian xử lý đã trôi qua dựa trên
monotonic clock trước khi gọi `setTime()`.

Không đặt đồng hồ về timestamp server tại `T3` một cách trực tiếp vì đó
là thời điểm server gửi response, đã cũ khi packet tới client.

Nên tính toán thời gian mục tiêu theo dạng:

``` text
targetEpochMillis =
    estimatedServerEpochAtReceive
    + elapsedProcessingTimeMillis
```

Việc tính toán phải tránh overflow, kiểm tra timestamp hợp lệ và dùng độ
chính xác millisecond.

### 5.6. Timezone

NTP cung cấp thời gian UTC, không cung cấp múi giờ địa phương.

Cấu hình timezone thiết bị riêng là:

``` text
Asia/Ho_Chi_Minh
```

Nếu cần ép timezone ở cấp hệ thống, có thể xem xét
`DevicePolicyManager.setTimeZone()` (API 28+) và điều kiện automatic
time zone tương ứng. Cần kiểm tra trên firmware mục tiêu.

Không cộng thêm 7 giờ vào timestamp trước khi gọi `setTime()`. Unix
epoch biểu diễn thời điểm tuyệt đối; Android hiển thị giờ địa phương
theo timezone.

### 5.7. Xác minh sau đồng bộ

Sau khi `setTime()` trả về `true`:

1.  Đọc `System.currentTimeMillis()`.
2.  So sánh với thời gian mục tiêu, tính cả thời gian thực thi.
3.  Kiểm tra sai số sau đặt trong ngưỡng chấp nhận.
4.  Ghi log `CLOCK_SYNC_SUCCESS` nếu đạt.
5.  Nếu DPM trả về false hoặc ném exception, ghi `CLOCK_SYNC_FAILED` /
    `CLOCK_SYNC_DENIED`.

Phải phân biệt rõ:

-   `NTP_SUCCESS`: lấy được thời gian từ server.
-   `CLOCK_SYNC_SUCCESS`: đã thay đổi System Clock và xác minh thành
    công.
-   `CLOCK_SYNC_DENIED`: thiếu quyền hoặc policy không cho phép.
-   `NTP_FAILED`: không kết nối hoặc response không hợp lệ.
-   `CLOCK_SYNC_VERIFY_FAILED`: API báo thành công nhưng phép kiểm tra
    sau đó không đạt.

------------------------------------------------------------------------

## 6. Firmware và khả năng tương thích Redmi Pad 2

### 6.1. Thông tin phần cứng/phần mềm đã biết

Theo trang thông số Xiaomi Việt Nam, REDMI Pad 2 được công bố sử dụng
Xiaomi HyperOS 2. Trang thông số cũng công bố nền tảng MediaTek Helio
G100-Ultra và Wi-Fi 5.

Thông tin này không xác định chính xác Android API level hoặc build
firmware đang cài trên từng máy. Firmware có thể khác theo khu vực, biến
thể thiết bị và cập nhật OTA.

### 6.2. Thông tin bắt buộc thu thập trên thiết bị

Trước khi phát triển phần tích hợp DPC, ghi lại trên một tablet thử
nghiệm:

  Trường                 Cách lấy
  ---------------------- ------------------------------------------
  Model                  Settings → About tablet
  Android version        Settings → About tablet
  HyperOS version        Settings → About tablet
  Build number           Settings → About tablet
  Security patch level   Android security update
  API level              `adb shell getprop ro.build.version.sdk`
  Build fingerprint      `adb shell getprop ro.build.fingerprint`
  Product/device         `adb shell getprop ro.product.device`
  Region/ROM             Build fingerprint và mã ROM

ADB kiểm tra nhanh:

``` bash
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.fingerprint
adb shell getprop ro.product.device
adb shell getprop ro.build.version.security_patch
```

Không suy luận API level chỉ từ tên HyperOS. Phải lấy giá trị trực tiếp
từ thiết bị.

### 6.3. Ma trận kiểm chứng firmware

  Hạng mục                    Điều kiện đạt
  --------------------------- ------------------------------------------------
  APK cài đặt                 Cài và khởi chạy được
  DPC receiver                Hệ thống nhận diện đúng receiver
  Device Owner provisioning   Thiết lập thành công
  `isDeviceOwnerApp()`        Trả về true
  Tắt Automatic Time          Có thể tắt và giữ trạng thái
  DPM `setTime()`             Trả về true
  System Clock                Giờ hệ thống thay đổi thực tế
  Timezone                    Hiển thị đúng `Asia/Ho_Chi_Minh`
  Reboot                      Ứng dụng vẫn giữ vai trò Device Owner
  WorkManager                 Công việc định kỳ được lập lịch và chạy
  Wi-Fi reconnect             Có thể đồng bộ lại sau khi mất mạng
  Firmware OTA                Chính sách và quyền vẫn hoạt động sau cập nhật

### 6.4. Điều kiện go/no-go

Chỉ chấp nhận Redmi Pad 2 làm nền tảng triển khai khi kiểm chứng được cả
ba điều kiện cốt lõi:

1.  Có thể provision ứng dụng thành Device Owner.
2.  Firmware cho phép DPM `setTime()` hoạt động với automatic time tắt.
3.  Sau reboot, ứng dụng vẫn là Device Owner và tiếp tục đặt được giờ.

Nếu một trong các điều kiện này không đạt, dừng triển khai hàng loạt.
Khi đó cần đánh giá firmware khác, giải pháp OEM/EMM hoặc quyền hệ
thống; không mặc định rằng root là bắt buộc.

------------------------------------------------------------------------

## 7. Provisioning Device Owner

### 7.1. Thiết bị thử nghiệm bằng ADB

Đây là phương pháp kiểm chứng kỹ thuật ban đầu, không phải quy trình
người dùng cuối thông thường.

Điều kiện: - Tablet đã factory reset hoặc ở trạng thái cho phép đặt
Device Owner. - Cài APK DPC lên thiết bị. - USB debugging/ADB được bật
trong quá trình thử nghiệm. - Dùng đúng component receiver đã khai báo.

Ví dụ lệnh (thay package và receiver theo project thực tế):

``` bash
adb install app-debug.apk

adb shell dpm set-device-owner \
  com.company.ntpclock/.admin.NtpDeviceAdminReceiver
```

Kiểm tra:

``` bash
adb shell dumpsys device_policy
```

Có thể dùng Test DPC để hiểu quy trình Device Owner trước khi tự phát
triển DPC. Không sử dụng Test DPC làm ứng dụng production.

### 7.2. Triển khai nhiều thiết bị

Các tablet công ty cần được quản lý theo mô hình company-owned / fully
managed hoặc dedicated device phù hợp.

Phương án giai đoạn đầu:

1.  Chuẩn hóa firmware và build trên các tablet.
2.  Factory reset theo quy trình đã được công ty phê duyệt.
3.  Provision DPC thành Device Owner.
4.  Cài bản release APK.
5.  Cấu hình NTP server và timezone.
6.  Chạy kiểm thử đặt giờ.
7.  Ghi nhận serial/model/build và kết quả triển khai.

QR provisioning là hướng phù hợp cho triển khai Android Enterprise,
nhưng cần thiết kế đúng quy trình DPC/EMM và provisioning extras. Không
được giả định chỉ cần tạo một QR chứa đường dẫn APK là mọi Redmi Pad 2
có thể tự động thành Device Owner trong môi trường offline.

Nếu không sử dụng EMM/cloud, có thể xây dựng quy trình provision nội bộ
qua ADB/workstation hoặc nghiên cứu DPC custom provisioning phù hợp với
firmware và yêu cầu mạng tại bước setup. Phải thử nghiệm
factory-reset-to-ready trên thiết bị thật trước khi chuẩn hóa quy trình.

**Cảnh báo:** Factory reset xóa dữ liệu trên thiết bị. Chỉ thực hiện
trên máy thử nghiệm hoặc theo kế hoạch triển khai được phê duyệt.

------------------------------------------------------------------------

## 8. Cấu trúc thư mục project

Tên project đề xuất: `NtpClock`.

``` text
NtpClock/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/company/ntpclock/
│       │   │   ├── NtpClockApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   │
│       │   │   ├── core/
│       │   │   │   ├── common/
│       │   │   │   ├── time/
│       │   │   │   │   ├── ClockProvider.kt
│       │   │   │   │   ├── TimeSource.kt
│       │   │   │   │   └── TimeZoneManager.kt
│       │   │   │   ├── network/
│       │   │   │   │   ├── NetworkMonitor.kt
│       │   │   │   │   └── UdpTransport.kt
│       │   │   │   └── logging/
│       │   │   │
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── NtpDatabase.kt
│       │   │   │   │   ├── SyncHistoryDao.kt
│       │   │   │   │   └── SyncHistoryEntity.kt
│       │   │   │   ├── preferences/
│       │   │   │   │   └── NtpSettingsDataStore.kt
│       │   │   │   └── repository/
│       │   │   │       ├── NtpRepositoryImpl.kt
│       │   │   │       └── SyncHistoryRepositoryImpl.kt
│       │   │   │
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   ├── repository/
│       │   │   │   └── usecase/
│       │   │   │       ├── QueryNtpTimeUseCase.kt
│       │   │   │       ├── CalculateOffsetUseCase.kt
│       │   │   │       ├── SyncSystemClockUseCase.kt
│       │   │   │       └── VerifyClockSyncUseCase.kt
│       │   │   │
│       │   │   ├── ntp/
│       │   │   │   ├── NtpClient.kt
│       │   │   │   ├── NtpPacket.kt
│       │   │   │   ├── NtpTimestamp.kt
│       │   │   │   ├── NtpResponseValidator.kt
│       │   │   │   └── NtpSampleSelector.kt
│       │   │   │
│       │   │   ├── admin/
│       │   │   │   ├── NtpDeviceAdminReceiver.kt
│       │   │   │   ├── DeviceOwnerManager.kt
│       │   │   │   ├── SystemClockController.kt
│       │   │   │   └── AdminPolicyManager.kt
│       │   │   │
│       │   │   ├── worker/
│       │   │   │   ├── NtpSyncWorker.kt
│       │   │   │   ├── SyncWorkScheduler.kt
│       │   │   │   └── BootReceiver.kt
│       │   │   │
│       │   │   ├── sync/
│       │   │   │   ├── NtpSyncCoordinator.kt
│       │   │   │   ├── SyncResult.kt
│       │   │   │   └── SyncState.kt
│       │   │   │
│       │   │   └── presentation/
│       │   │       ├── dashboard/
│       │   │       ├── settings/
│       │   │       ├── history/
│       │   │       ├── diagnostics/
│       │   │       ├── navigation/
│       │   │       └── components/
│       │   │
│       │   └── res/
│       │       ├── xml/
│       │       │   └── device_admin_policies.xml
│       │       ├── values/
│       │       └── drawable/
│       │
│       ├── test/
│       │   └── java/com/company/ntpclock/
│       └── androidTest/
│           └── java/com/company/ntpclock/
│
├── docs/
│   ├── spec-project.md
│   ├── device-provisioning.md
│   ├── firmware-compatibility.md
│   └── test-plan.md
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

### 8.1. Trách nhiệm các module

-   `ntp/`: chỉ xử lý protocol NTP, packet, timestamp và validation;
    không gọi Android UI.
-   `sync/`: điều phối toàn bộ một lần đồng bộ.
-   `admin/`: kiểm tra Device Owner, policy và gọi API chỉnh giờ.
-   `worker/`: chạy đồng bộ nền và xử lý sau boot.
-   `data/`: lưu cấu hình và lịch sử.
-   `domain/`: nghiệp vụ độc lập với UI/framework.
-   `presentation/`: màn hình và trạng thái giao diện.
-   `core/time/`: abstraction đọc wall clock, monotonic clock và
    timezone.

------------------------------------------------------------------------

## 9. Luồng đồng bộ chi tiết

### 9.1. Đồng bộ thủ công

``` text
Người dùng nhấn "Đồng bộ ngay"
        |
        v
Kiểm tra Wi-Fi/LAN
        |
        v
Kiểm tra Device Owner
        |
        v
Kiểm tra AUTO_TIME = OFF
        |
        v
Gửi 5 NTP requests
        |
        v
Validate và chọn mẫu tốt
        |
        v
Tính server time tại thời điểm hiện tại
        |
        v
Kiểm tra offset / threshold
        |
        v
DPM.setTime(admin, targetEpochMillis)
        |
        v
Đọc lại System Clock
        |
        v
Lưu lịch sử + cập nhật Dashboard
```

### 9.2. Đồng bộ tự động

-   WorkManager PeriodicWorkRequest với chu kỳ 60 phút.
-   Android WorkManager yêu cầu chu kỳ định kỳ tối thiểu 15 phút; thời
    điểm chạy không đảm bảo chính xác từng phút.
-   Doze, pin, tải hệ thống và chính sách HyperOS có thể trì hoãn công
    việc.
-   Dùng unique periodic work để tránh tạo nhiều lịch trùng.
-   Sau reboot, WorkManager duy trì lịch đã đăng ký; có thể bổ sung
    BootReceiver nếu cần yêu cầu chạy sớm, nhưng phải tuân thủ giới hạn
    background execution.
-   Khi tablet vừa kết nối lại LAN, có thể enqueue một one-time sync có
    điều kiện network.
-   Không chạy vòng lặp foreground service liên tục chỉ để kiểm tra giờ.

### 9.3. Chính sách khi lỗi

  -----------------------------------------------------------------------
  Tình huống                          Hành vi
  ----------------------------------- -----------------------------------
  Không có Wi-Fi/LAN                  Không chỉnh giờ; báo mất kết nối

  UDP 123 timeout                     Retry giới hạn; ghi `NTP_FAILED`

  NTP response không hợp lệ           Loại response; không chỉnh giờ

  Không phải Device Owner             Báo `CLOCK_SYNC_DENIED`

  Automatic Time đang bật             Không gọi đặt giờ; báo cần xử lý
                                      policy

  DPM trả về false                    Ghi lỗi và giữ nguyên trạng thái

  Thiết bị reboot                     Lập lịch/chạy lại theo chính sách

  Server không khả dụng               Giữ giờ hiện tại, không dùng dữ
                                      liệu cache cũ để chỉnh
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 10. Giao diện ứng dụng

### 10.1. Dashboard

Hiển thị:

-   Giờ hệ thống hiện tại.
-   Ngày hiện tại.
-   NTP server và port.
-   Trạng thái kết nối LAN.
-   Trạng thái Device Owner.
-   Trạng thái Automatic Time.
-   Độ lệch đo được.
-   Thời điểm đồng bộ thành công gần nhất.
-   Nút **Đồng bộ ngay**.

### 10.2. Settings

Các cấu hình:

  Cấu hình                Giá trị mặc định
  ----------------------- --------------------
  NTP Host                `192.168.4.2`
  NTP Port                `123`
  Timezone                `Asia/Ho_Chi_Minh`
  Sync interval           60 phút
  Sample count            5
  Timeout                 2500 ms
  Max acceptable RTT      Cấu hình được
  Max offset before set   Cấu hình được
  Auto sync               Enabled
  Sync on boot            Enabled

Không cho người dùng thông thường thay đổi NTP host nếu thiết bị đã được
công ty quản lý; nên có chế độ quản trị riêng.

### 10.3. History

Mỗi lần chạy lưu:

-   Thời điểm chạy theo wall clock.
-   NTP server.
-   RTT.
-   Offset.
-   Stratum / Leap Indicator.
-   Giờ trước đồng bộ.
-   Giờ mục tiêu.
-   Giờ sau đồng bộ.
-   Kết quả API.
-   Mã trạng thái và thông tin lỗi.

------------------------------------------------------------------------

## 11. Bảo mật và vận hành mạng nội bộ

-   Tablet chỉ cần truy cập địa chỉ NTP nội bộ `192.168.4.2`, UDP 123.
-   Không yêu cầu Internet hoặc public DNS trong lúc vận hành.
-   Wi-Fi/VLAN phải cho phép client-to-server UDP 123.
-   Kiểm tra AP/client isolation; nếu bật, tablet có thể không truy cập
    được server dù vẫn kết nối Wi-Fi.
-   Firewall Windows phải cho phép inbound UDP 123 từ subnet thiết bị.
-   Giới hạn nguồn NTP được cấu hình ở client; không nhận thời gian từ
    host bất kỳ.
-   Không mở UDP 123 ra Internet nếu không có yêu cầu và biện pháp bảo
    vệ.
-   NTP cơ bản không có xác thực mật mã; bảo vệ bằng phân đoạn mạng,
    firewall và kiểm soát máy chủ.
-   Không ghi dữ liệu nhạy cảm vào log.
-   Bảo vệ release keystore và quy trình ký APK.
-   Chỉ quản trị viên được thay đổi policy Device Owner.

------------------------------------------------------------------------

## 12. Kiểm thử và tiêu chí nghiệm thu

### 12.1. Kiểm thử kỹ thuật

  -----------------------------------------------------------------------
  ID                      Test                    Kết quả mong đợi
  ----------------------- ----------------------- -----------------------
  T01                     Tablet truy cập server  Nhận NTP response
                          UDP 123                 

  T02                     Server không hoạt động  Báo lỗi, không đổi giờ

  T03                     Packet sai originate    Loại packet
                          timestamp               

  T04                     NTP server              Không đổi giờ
                          unsynchronized          

  T05                     App không phải Device   Không đặt giờ
                          Owner                   

  T06                     Automatic Time ON       Không đặt giờ; báo
                                                  policy

  T07                     Device Owner +          `setTime()` có thể
                          Automatic Time OFF      thành công

  T08                     Đặt giờ lệch 5 phút     System Clock được chỉnh
                                                  về gần giờ server

  T09                     Đặt timezone Việt Nam   Hiển thị giờ UTC+7 đúng

  T10                     Reboot tablet           DPC vẫn là Device Owner

  T11                     Mất Wi-Fi               Không dùng thời gian
                                                  cache để chỉnh

  T12                     Wi-Fi trở lại           Có thể đồng bộ lại

  T13                     WorkManager 60 phút     Công việc được thực thi
                                                  trong điều kiện hệ
                                                  thống cho phép

  T14                     Nhiều tablet cùng sync  Tất cả lấy thời gian từ
                                                  cùng server

  T15                     OTA firmware            Kiểm tra lại quyền,
                                                  policy và scheduler
  -----------------------------------------------------------------------

### 12.2. Tiêu chí nghiệm thu đề xuất

Các ngưỡng sau là mục tiêu ban đầu, phải xác nhận theo yêu cầu hệ thống
sản xuất:

-   Tất cả tablet lấy thời gian từ đúng `192.168.4.2`.
-   Không có lần chỉnh giờ nào khi NTP response chưa được xác thực.
-   System Clock sau đồng bộ sai lệch không quá ±1 giây so với thời gian
    tham chiếu tại cùng thời điểm đo.
-   Đồng bộ thủ công hoạt động ổn định.
-   Đồng bộ định kỳ hoạt động khi Android cho phép worker chạy.
-   Lịch sử thể hiện rõ lấy giờ thành công và đặt giờ thành công là hai
    trạng thái riêng.
-   Sau reboot, DPC vẫn giữ vai trò Device Owner.
-   Firmware được nghiệm thu phải có hồ sơ build fingerprint.

Không cam kết độ chính xác ±1 giây chỉ từ phép thử `w32tm` hiện tại;
phải đo trên tablet trong điều kiện mạng thực tế.

------------------------------------------------------------------------

## 13. Kế hoạch triển khai

### Phase 0 -- Khảo sát firmware và quyền

1.  Thu thập Android version, HyperOS version, API level và build
    fingerprint.
2.  Factory reset một thiết bị thử nghiệm theo quy trình được phê duyệt.
3.  Cài DPC thử nghiệm.
4.  Provision Device Owner bằng ADB.
5.  Xác nhận `isDeviceOwnerApp()`.
6.  Tắt Automatic Time.
7.  Thử gọi `DevicePolicyManager.setTime()`.
8.  Kiểm tra đồng hồ hệ thống sau reboot.

**Gate:** Chỉ tiếp tục nếu đặt giờ thực tế thành công trên firmware
Redmi Pad 2 mục tiêu.

### Phase 1 -- NTP core

1.  Tạo project Kotlin.
2.  Xây dựng UDP transport.
3.  Xây dựng NTP packet parser.
4.  Xây dựng timestamp conversion.
5.  Xây dựng validation.
6.  Tính offset/delay.
7.  Viết unit test cho packet và timestamp.

### Phase 2 -- System clock integration

1.  Tích hợp DeviceAdminReceiver.
2.  Xây dựng DeviceOwnerManager.
3.  Kiểm tra automatic time policy.
4.  Xây dựng SystemClockController.
5.  Gọi DPM setTime.
6.  Xác minh giờ hệ thống.
7.  Ghi lịch sử kết quả.

### Phase 3 -- UI và persistence

1.  Dashboard.
2.  Settings.
3.  History.
4.  Diagnostics.
5.  Room và DataStore.

### Phase 4 -- Background sync

1.  WorkManager 60 phút.
2.  Unique periodic work.
3.  Retry/backoff.
4.  Reconnect trigger.
5.  Boot behavior.
6.  Kiểm tra giới hạn nền của HyperOS.

### Phase 5 -- Pilot và rollout

1.  Pilot 1 thiết bị.
2.  Pilot 2--5 thiết bị.
3.  Kiểm thử nhiều thiết bị cùng lúc.
4.  Chuẩn hóa firmware.
5.  Chuẩn hóa provisioning.
6.  Phát hành APK release.
7.  Triển khai theo từng đợt.

------------------------------------------------------------------------

## 14. Rủi ro và quyết định kỹ thuật

  -----------------------------------------------------------------------
  Rủi ro                  Ảnh hưởng               Hướng xử lý
  ----------------------- ----------------------- -----------------------
  HyperOS chặn hoặc hạn   Không thể đặt giờ qua   Kiểm chứng firmware
  chế Device Owner        DPM                     trước khi phát triển
  provisioning                                    sâu

  Automatic Time không    DPM setTime không có    Xử lý policy bằng DPC
  tắt được                hiệu lực                được hỗ trợ

  Wi-Fi client isolation  Không truy cập NTP      Điều chỉnh
                          server                  VLAN/AP/firewall

  WorkManager bị trì hoãn Không chạy đúng phút    Chấp nhận lịch không
                          thứ 60                  chính xác tuyệt đối;
                                                  kiểm thử HyperOS

  NTP server sai giờ      Tất cả tablet cùng sai  Giám sát W32Time và
                                                  nguồn upstream của
                                                  server

  Người dùng thay đổi giờ Sai lệch giữa các       DPC hạn chế cấu hình
  thủ công                tablet                  ngày giờ nếu policy phù
                                                  hợp

  Firmware OTA            Thay đổi hành vi quản   Kiểm thử lại trên từng
                          trị                     build

  NTP spoofing trong LAN  Có thể đặt sai giờ      Phân đoạn mạng,
                                                  firewall, kiểm soát
                                                  server
  -----------------------------------------------------------------------

### Quyết định quan trọng

1.  **Không xây backend riêng:** Android kết nối trực tiếp Windows NTP
    Server.
2.  **Không dùng quyền Admin thông thường:** bắt buộc xác nhận Device
    Owner.
3.  **Không root ở phương án ưu tiên:** chỉ đánh giá root/OEM nếu DPM
    không được firmware hỗ trợ.
4.  **Không triển khai hàng loạt trước Phase 0:** firmware là điều kiện
    chặn (go/no-go).
5.  **Không dùng thời gian cache để chỉnh khi server mất kết nối.**
6.  **Không xem WorkManager 60 phút là bộ hẹn giờ chính xác tuyệt đối.**

------------------------------------------------------------------------

## 15. Tài liệu tham khảo

1.  Android `DevicePolicyManager` API -- `setTime()`, `setTimeZone()` và
    các policy thời gian:\
    https://developer.android.com/reference/android/app/admin/DevicePolicyManager

2.  Android Enterprise -- Fully managed device:\
    https://developers.google.com/android/work/requirements/fully-managed-device

3.  Android Management API -- Provision device:\
    https://developers.google.com/android/management/provision-device

4.  Android Dedicated Devices -- Provision và quản lý thiết bị chuyên
    dụng:\
    https://developer.android.com/work/dpc/dedicated-devices

5.  Xiaomi Việt Nam -- REDMI Pad 2 specifications:\
    https://www.mi.com/vn/product/redmi-pad-2/specs/

6.  RFC 5905 -- Network Time Protocol Version 4:\
    https://www.rfc-editor.org/rfc/rfc5905

7.  Android WorkManager -- Periodic work:\
    https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work

------------------------------------------------------------------------

## 16. Kết luận

Giải pháp mục tiêu là một Android APK Kotlin tích hợp NTP Client và
Device Policy Controller. APK lấy thời gian trực tiếp từ Windows Server
`192.168.4.2` qua UDP 123, tính thời gian chuẩn, sau đó dùng
`DevicePolicyManager.setTime()` để cập nhật Android System Clock.

**Điều kiện kỹ thuật quan trọng nhất chưa được xác nhận là khả năng
provisioning Device Owner và thực thi `setTime()` trên đúng bản firmware
Redmi Pad 2 của công ty.** Đây là việc cần thử nghiệm đầu tiên. Khi thử
nghiệm đạt, có thể hoàn thiện APK và triển khai cho nhiều tablet mà
không cần backend riêng.
