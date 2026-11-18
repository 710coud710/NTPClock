# Thiết kế và triển khai Foreground Service cho ứng dụng NTPClock

## 1. Mục tiêu

Foreground Service (FGS) là thành phần Android cho phép ứng dụng tiếp
tục thực hiện tác vụ nền trong khi người dùng không mở giao diện ứng
dụng. Với NTPClock, FGS có thể điều phối việc truy vấn NTP Server nội bộ
theo chu kỳ, cập nhật trạng thái đồng bộ và hiển thị thông báo thường
trực.

Cần phân biệt hai vấn đề độc lập:

1.  **Chạy tác vụ nền:** Foreground Service hoặc WorkManager đảm nhiệm
    việc lên lịch/thực hiện tác vụ khi Activity không còn hiển thị.
2.  **Thay đổi System Clock:** ứng dụng cần quyền hệ thống phù hợp.
    Foreground Service không tự cấp quyền chỉnh giờ Android.

Trên Redmi Pad 2 hiện tại, kết quả kiểm tra `dumpsys device_policy` cho
thấy ứng dụng chưa được cấp Device Owner hoặc Profile Owner. Vì vậy,
Service có thể chạy nhưng thao tác thay đổi giờ hệ thống vẫn có thể bị
từ chối (`CLOCK_SYNC_DENIED`).

## 2. Kiến trúc đề xuất

``` mermaid
flowchart TD
    A["NTPClock Android App"] --> B["MainActivity"]
    A --> C["BootReceiver"]
    B --> D["NtpForegroundService"]
    C --> D
    D --> E["NtpClient"]
    D --> F["TimeSyncManager"]
    E --> G["NTP Server 192.168.4.2"]
    F --> H["Android System Clock"]
    D --> I["Room / DataStore"]
```

### Vai trò các thành phần

  -----------------------------------------------------------------------
  Thành phần                          Trách nhiệm
  ----------------------------------- -----------------------------------
  `MainActivity`                      Giao diện, cấu hình NTP Server,
                                      bật/tắt đồng bộ

  `NtpForegroundService`              Điều phối tác vụ nền, Notification
                                      và chu kỳ đồng bộ

  `NtpClient`                         Gửi UDP NTP request đến máy chủ nội
                                      bộ và tính offset

  `TimeSyncManager`                   Kiểm tra quyền, yêu cầu chỉnh giờ
                                      và xác minh kết quả

  `BootReceiver`                      Khôi phục tác vụ sau khi thiết bị
                                      khởi động lại

  `Room`                              Lưu lịch sử đồng bộ

  `DataStore`                         Lưu cấu hình và trạng thái bật/tắt
                                      tự động
  -----------------------------------------------------------------------

Nguyên tắc thiết kế: Service chỉ điều phối vòng đời tác vụ. Không đặt
toàn bộ thuật toán NTP hoặc logic chỉnh giờ trực tiếp trong Service.
Cách tách lớp này giúp tái sử dụng NTP Client cho WorkManager, kiểm thử
và xử lý lỗi độc lập.

## 3. Foreground Service hoạt động như thế nào?

### 3.1. Vòng đời cơ bản

1.  Người dùng mở NTPClock và bật chế độ đồng bộ tự động.
2.  `MainActivity` yêu cầu Android khởi chạy `NtpForegroundService`.
3.  Service gọi `startForeground()` và hiển thị Notification trong thời
    hạn Android quy định.
4.  Service thực hiện một lần đồng bộ NTP.
5.  Service cập nhật trạng thái và lịch sử.
6.  Nếu thiết kế dùng FGS chạy thường trực, Service chờ chu kỳ tiếp theo
    rồi thực hiện lại.
7.  Khi người dùng tắt chế độ tự động, ứng dụng yêu cầu dừng Service.

Không nên phụ thuộc vào Activity để duy trì tác vụ. Activity có thể bị
hủy khi người dùng thoát ứng dụng, trong khi Service có vòng đời riêng.

### 3.2. Notification

Foreground Service bắt buộc phải hiển thị Notification trong thời gian
hoạt động. Notification nên thể hiện thông tin hữu ích, chẳng hạn:

-   Tên ứng dụng: NTPClock.
-   Trạng thái: đang hoạt động, đang đồng bộ, thành công hoặc lỗi.
-   NTP Server: `192.168.4.2`.
-   Thời điểm đồng bộ gần nhất.
-   Thời điểm dự kiến đồng bộ tiếp theo (nếu có thể xác định).

Chỉ cập nhật Notification khi trạng thái thay đổi hoặc sau một lần đồng
bộ; không cần cập nhật từng giây.

Ví dụ nội dung:

``` text
NTPClock
Đang chạy nền · Đồng bộ thời gian

Trạng thái: Đang hoạt động
NTP Server: 192.168.4.2
Lần đồng bộ gần nhất: 08:00:00
Lần tiếp theo dự kiến: 09:00:00
```

Trên Android 13 trở lên, quyền thông báo `POST_NOTIFICATIONS` cần được
xử lý theo quy định của hệ điều hành. Việc người dùng từ chối quyền
thông báo không nên bị hiểu nhầm là đã cấp quyền chỉnh giờ hệ thống.

### 3.3. BootReceiver

Android dừng tiến trình và Service khi thiết bị khởi động lại. Nếu cần
tự khôi phục tác vụ:

1.  Khai báo receiver nhận `BOOT_COMPLETED`.
2.  Khi nhận broadcast, đọc cấu hình đồng bộ tự động trong DataStore.
3.  Nếu người dùng đã bật tự động, lập lịch lại công việc hoặc khởi chạy
    thành phần được phép.
4.  Nếu chế độ tự động đã tắt, không tự khởi chạy.

Việc khởi chạy Foreground Service trực tiếp từ `BOOT_COMPLETED` bị giới
hạn theo loại Service và phiên bản Android. Đặc biệt, Android 15 áp dụng
thêm hạn chế đối với một số loại Foreground Service. Vì vậy, không nên
thiết kế dựa trên giả định rằng mọi FGS đều có thể khởi chạy từ boot
receiver.

WorkManager thường thuận tiện hơn cho tác vụ định kỳ vì lịch làm việc
được lưu và quản lý qua các lần khởi động lại.

## 4. Android quản lý tác vụ nền

### 4.1. Hành vi theo tình huống

  -----------------------------------------------------------------------
  Tình huống                          Hành vi dự kiến
  ----------------------------------- -----------------------------------
  Nhấn Home                           FGS thường tiếp tục chạy

  Chuyển sang ứng dụng khác           FGS thường tiếp tục chạy

  Tắt màn hình/khóa màn hình          Có thể tiếp tục, nhưng vẫn chịu
                                      quản lý năng lượng

  Vuốt ứng dụng khỏi Recent Apps      Thường không đồng nghĩa với Force
                                      Stop; hành vi còn phụ thuộc
                                      firmware và cấu hình

  Người dùng nhấn Force Stop          Ứng dụng bị dừng; không thể tự khởi
                                      chạy lại cho đến khi người dùng mở
                                      ứng dụng

  Khởi động lại thiết bị              Service đang chạy bị dừng; cần khôi
                                      phục theo cơ chế phù hợp

  Mất Wi-Fi                           Tác vụ cần ghi nhận lỗi và thử lại
                                      sau

  Android vào Doze                    Tác vụ mạng và lịch nền có thể bị
                                      trì hoãn
  -----------------------------------------------------------------------

Foreground Service không đảm bảo tiến trình tồn tại vĩnh viễn hoặc tác
vụ được chạy đúng từng phút. Hệ điều hành vẫn có thể dừng ứng dụng trong
các trường hợp đặc biệt, chẳng hạn người dùng Force Stop hoặc hệ thống
cần xử lý tài nguyên.

### 4.2. Doze Mode

Doze là cơ chế tiết kiệm pin của Android khi thiết bị không được sử dụng
trong thời gian dài. Khi vào Doze, Android có thể:

-   Hạn chế truy cập mạng.
-   Trì hoãn tác vụ nền.
-   Hạn chế hoạt động CPU.
-   Gom các tác vụ chờ vào các khoảng thời gian bảo trì.

Foreground Service có mức ưu tiên cao hơn tác vụ nền thông thường, nhưng
không đảm bảo NTP request luôn được thực hiện đúng thời điểm dự kiến.

Ví dụ: chu kỳ đồng bộ đặt là 60 phút nhưng thiết bị đang ở trạng thái
ngủ sâu, lần đồng bộ tiếp theo có thể bị trễ.

### 4.3. Xiaomi HyperOS

Trên Redmi Pad 2, cần kiểm tra thêm các thiết lập quản lý ứng dụng của
HyperOS:

  -----------------------------------------------------------------------
  Thiết lập                           Cấu hình nên kiểm tra
  ----------------------------------- -----------------------------------
  Autostart                           Cho phép NTPClock tự khởi chạy nếu
                                      firmware cung cấp tùy chọn

  Battery Saver                       Chọn không hạn chế đối với ứng dụng
                                      nếu có

  Background activity                 Cho phép hoạt động nền

  Notification                        Cho phép thông báo

  Wi-Fi khi ngủ                       Kiểm tra tùy chọn duy trì kết nối
                                      Wi-Fi khi màn hình tắt
  -----------------------------------------------------------------------

Tên menu và khả năng cấu hình có thể khác nhau theo phiên bản HyperOS,
khu vực firmware và chính sách quản trị thiết bị.

## 5. Khai báo AndroidManifest.xml

Ví dụ khai báo nền tảng ban đầu:

``` xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application>

        <service
            android:name=".service.NtpForegroundService"
            android:exported="false" />

        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>

    </application>
</manifest>
```

Đây chỉ là khung minh họa, chưa phải Manifest hoàn chỉnh để phát hành.
Cần xác định chính xác loại Foreground Service và quyền tương ứng dựa
trên chức năng thực tế và `targetSdk`.

### Lưu ý Android 14 và Android 15

-   Khi target Android 14 trở lên, ứng dụng phải khai báo loại
    Foreground Service phù hợp và quyền tương ứng.
-   Không chọn `dataSync` một cách máy móc chỉ vì ứng dụng đồng bộ dữ
    liệu. Android 15 áp dụng giới hạn thời lượng cho một số loại FGS,
    trong đó có `dataSync`.
-   Android 15 bổ sung hạn chế khởi chạy một số loại FGS từ
    `BOOT_COMPLETED`.
-   `specialUse` chỉ nên xem xét khi trường hợp sử dụng thực sự phù hợp;
    cần khai báo subtype và đáp ứng yêu cầu nền tảng/chính sách phân
    phối. Không dùng nó như cách né giới hạn.
-   `POST_NOTIFICATIONS` là quyền liên quan đến hiển thị thông báo,
    không phải quyền chỉnh System Clock.
-   Không thể cấp quyền `SET_TIME` cho ứng dụng thông thường bằng
    `adb shell pm grant`; đây không phải quyền runtime mà người dùng có
    thể cấp theo cách thông thường.

Cần đối chiếu tài liệu Android theo đúng `compileSdk`, `targetSdk` và
loại Service trước khi hoàn thiện cấu hình phát hành.

## 6. Foreground Service hay WorkManager?

Nếu yêu cầu chỉ là đồng bộ một lần mỗi 60 phút, WorkManager thường là
lựa chọn phù hợp hơn.

  -----------------------------------------------------------------------
  Tiêu chí                Foreground Service      WorkManager
  ----------------------- ----------------------- -----------------------
  Chạy khi Activity đóng  Có                      Có

  Notification thường     Bắt buộc khi chạy FGS   Không bắt buộc cho
  trực                                            Worker thông thường

  Tác vụ định kỳ          Tự điều phối chu kỳ     Có Periodic Work

  Đúng chính xác từng     Không đảm bảo           Không đảm bảo
  phút                                            

  Ảnh hưởng Doze          Vẫn chịu một số giới    Có thể bị trì hoãn
                          hạn                     

  Tác vụ kéo dài          Phù hợp hơn nếu đúng    Không phải mục tiêu
                          loại FGS                chính

  Quản lý lịch qua reboot Cần tự thiết kế/khôi    Được WorkManager quản
                          phục                    lý

  Mức độ phức tạp         Cao hơn                 Thường thấp hơn
  -----------------------------------------------------------------------

### Phương án khuyến nghị cho NTPClock

**Dùng WorkManager làm bộ lập lịch chính** nếu chỉ cần đồng bộ định kỳ
60 phút:

-   `NtpSyncWorker`: thực hiện một lần đồng bộ rồi kết thúc.
-   `SyncScheduler`: tạo, cập nhật hoặc hủy lịch định kỳ.
-   `NtpClient`: truy vấn NTP Server.
-   `TimeSyncManager`: kiểm tra quyền và điều chỉnh giờ hệ thống.
-   `Room`: lưu lịch sử.
-   `DataStore`: lưu cấu hình.

Chỉ sử dụng Foreground Service khi có yêu cầu thực sự cần tiến trình
hoạt động thường trực, Notification liên tục hoặc tác vụ kéo dài. Không
nên tạo FGS chỉ để giữ một vòng lặp `delay(60 phút)` nếu WorkManager đáp
ứng được yêu cầu.

Lưu ý: Periodic WorkManager có chu kỳ tối thiểu 15 phút, nhưng thời điểm
chạy không chính xác tuyệt đối và có thể bị trì hoãn bởi Doze, điều kiện
mạng, pin hoặc chính sách hệ thống.

## 7. Luồng xử lý và phục hồi lỗi

``` mermaid
flowchart TD
    A["Start NTPClock"] --> B["Load configuration"]
    B --> C{"Auto Sync enabled?"}
    C -->|No| D["Stop background task"]
    C -->|Yes| E["Start scheduler / Service"]
    E --> F["Check network"]
    F -->|Offline| G["Record network error"]
    G --> H["Retry later"]
    F -->|Online| I["Send NTP request"]
    I --> J{"Valid NTP response?"}
    J -->|No| G
    J -->|Yes| K["Calculate time offset"]
    K --> L{"Can set system clock?"}
    L -->|No| M["Record CLOCK_SYNC_DENIED"]
    L -->|Yes| N["Adjust system clock"]
    N --> O["Verify system time"]
    M --> P["Save sync history"]
    O --> P
    P --> Q["Wait for next cycle"]
    Q --> F
```

### Chính sách Retry tham khảo

  Lần thử               Thời gian chờ
  --------- -------------------------
  Lần 1                        1 phút
  Lần 2                        5 phút
  Lần 3                       15 phút
  Lần 4                       30 phút
  Sau đó      Quay lại chu kỳ 60 phút

Nên giới hạn số lần retry, ghi log và tránh gửi request liên tục khi
mạng nội bộ bị mất.

### Các trạng thái nên lưu

  Trạng thái              Ý nghĩa
  ----------------------- ----------------------------------------------
  `NTP_SUCCESS`           Nhận được phản hồi NTP hợp lệ
  `NTP_FAILED`            Không nhận được phản hồi NTP hợp lệ
  `CLOCK_SYNC_SUCCESS`    Chỉnh giờ hệ thống thành công
  `CLOCK_SYNC_DENIED`     Không đủ quyền chỉnh giờ hệ thống
  `NETWORK_UNAVAILABLE`   Không có kết nối mạng phù hợp
  `VERIFY_FAILED`         Chỉnh giờ xong nhưng bước xác minh không đạt

Cần phân biệt `NTP_SUCCESS` và `CLOCK_SYNC_SUCCESS`: nhận được giờ từ
máy chủ không có nghĩa là đã thay đổi được System Clock.

## 8. Cấu trúc thư mục đề xuất

``` text
app/
└── src/main/
    ├── java/com/example/ntpclock/
    │   ├── data/
    │   │   ├── local/
    │   │   │   ├── AppDatabase.kt
    │   │   │   └── SyncHistoryDao.kt
    │   │   └── preferences/
    │   │       └── SettingsDataStore.kt
    │   │
    │   ├── domain/
    │   │   ├── NtpClient.kt
    │   │   ├── TimeSyncManager.kt
    │   │   └── SyncScheduler.kt
    │   │
    │   ├── service/
    │   │   └── NtpForegroundService.kt
    │   │
    │   ├── worker/
    │   │   └── NtpSyncWorker.kt
    │   │
    │   ├── receiver/
    │   │   └── BootReceiver.kt
    │   │
    │   ├── notification/
    │   │   └── NtpNotificationManager.kt
    │   │
    │   ├── ui/
    │   │   ├── home/
    │   │   ├── settings/
    │   │   └── history/
    │   │
    │   └── MainActivity.kt
    │
    └── AndroidManifest.xml
```

Nếu chọn WorkManager làm phương án chính, thư mục `service/` có thể chưa
cần triển khai. Có thể giữ lại cho giai đoạn sau nếu yêu cầu vận hành
thay đổi.

## 9. Quyền chỉnh System Clock -- vấn đề độc lập với Service

Đây là điều kiện quan trọng nhất của dự án.

-   Foreground Service không cấp quyền `SET_TIME`.
-   WorkManager cũng không cấp quyền `SET_TIME`.
-   Device Admin thông thường không đồng nghĩa với Device Owner.
-   Với thiết bị được quản lý đầy đủ, hướng Android Enterprise
    DPC/Device Owner có thể cho phép quản trị thời gian bằng API phù
    hợp.
-   Thiết bị đã provision thường không thể tùy ý biến một ứng dụng thông
    thường thành Device Owner; quy trình provisioning thường yêu cầu
    thiết bị ở trạng thái phù hợp và có thể cần factory reset.
-   Một lựa chọn khác là cấu hình dịch vụ thời gian hệ thống của
    firmware/OEM trỏ về NTP Server nội bộ, nếu firmware hỗ trợ.
-   Với triển khai sâu hơn, cần nhà sản xuất hoặc firmware/system app có
    quyền đặc biệt.

Do đó, cần kiểm chứng khả năng chỉnh giờ trên một thiết bị thử nghiệm
trước khi triển khai hàng loạt. Không nên xem việc Service chạy thành
công là bằng chứng đồng bộ System Clock đã thành công.

## 10. Kế hoạch kiểm thử trên Redmi Pad 2

  -----------------------------------------------------------------------
  Bài kiểm thử            Cách thực hiện          Kết quả cần xác nhận
  ----------------------- ----------------------- -----------------------
  Chạy nền                Bật đồng bộ rồi nhấn    Tác vụ vẫn được lập
                          Home                    lịch

  Đóng giao diện          Chuyển sang ứng dụng    Tác vụ không phụ thuộc
                          khác                    Activity

  Tắt màn hình            Để máy khóa qua một chu Ghi nhận thời điểm thực
                          kỳ                      tế tác vụ chạy

  Mất Wi-Fi               Tắt Wi-Fi rồi bật lại   Không crash; có retry

  NTP Server mất kết nối  Chặn truy cập đến       Ghi `NTP_FAILED`
                          `192.168.4.2`           

  Không có quyền chỉnh    Chạy trên cấu hình hiện Ghi `CLOCK_SYNC_DENIED`
  giờ                     tại                     

  Reboot                  Khởi động lại tablet    Lịch tự động được khôi
                                                  phục đúng quy định

  Force Stop              Buộc dừng ứng dụng      Xác nhận không tự chạy
                                                  lại cho đến khi mở app

  HyperOS Battery Saver   Thử chế độ hạn chế và   So sánh độ trễ và khả
                          không hạn chế           năng chạy nền

  Nhiều chu kỳ            Theo dõi trong 3--7     Đánh giá drift và tỷ lệ
                          ngày                    đồng bộ thành công
  -----------------------------------------------------------------------

Nên lưu tối thiểu: thời gian bắt đầu, thời gian kết thúc, NTP offset,
round-trip delay, trạng thái mạng, trạng thái chỉnh giờ và lỗi chi tiết.

## 11. Lộ trình triển khai đề xuất

1.  **Hoàn thiện bộ lập lịch:** triển khai WorkManager với chu kỳ 60
    phút.
2.  **Tái sử dụng NTP Client:** giữ nguyên logic truy vấn NTP hiện có,
    không viết lại nếu đã hoạt động đúng.
3.  **Tách TimeSyncManager:** quản lý quyền, thao tác chỉnh giờ và xác
    minh.
4.  **Xử lý trạng thái:** lưu lịch sử và phân biệt lỗi mạng, lỗi NTP,
    lỗi quyền.
5.  **Kiểm thử nền:** kiểm tra khi đóng app, tắt màn hình, mất Wi-Fi và
    reboot.
6.  **Giải quyết quyền hệ thống:** đánh giá Device Owner/DPC, cấu hình
    NTP hệ thống của OEM hoặc firmware.
7.  **Pilot:** thử nghiệm trên một tablet trong vài ngày trước khi nhân
    rộng.

## 12. Kết luận

Đối với NTPClock chạy trên nhiều Redmi Pad 2 và đồng bộ mỗi 60 phút:

-   WorkManager là lựa chọn mặc định hợp lý cho tác vụ định kỳ.
-   Foreground Service chỉ nên dùng khi thật sự cần tác vụ kéo dài và
    Notification thường trực.
-   HyperOS cần được cấu hình để hạn chế việc chặn hoạt động nền.
-   Boot và Force Stop phải được xử lý theo đúng giới hạn Android.
-   Quyền chỉnh System Clock là vấn đề riêng, không được giải quyết bằng
    Foreground Service.

**Ưu tiên kỹ thuật:** trước tiên chứng minh được ứng dụng có thể thực
hiện một lần đồng bộ NTP và thay đổi System Clock thành công trên thiết
bị thử nghiệm. Sau đó mới hoàn thiện lịch chạy nền và triển khai hàng
loạt.
