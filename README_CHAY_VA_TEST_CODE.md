# OFFLINE-FIRST TASK MANAGER – ĐỀ TÀI 01 (GIỮA KỲ)

**Nhóm:** 524H0117 – 524H0119  
**Môn học:** Phát triển ứng dụng di động  
**Đề tài:** Xây dựng ứng dụng Android hỗ trợ hoạt động ngoại tuyến và đồng bộ dữ liệu tự động.  
**Ứng dụng minh họa:** Offline-First Task Manager  
**Repository GitHub:** https://github.com/nguyengiaphat695-coder/524H0117_524H0119_GiuaKi_DeTai1

## 1. Giới thiệu và phạm vi sản phẩm

Dự án gồm **mã nguồn Android Kotlin**, **FastAPI Server**, **SQLite mẫu**, **APK debug**, **bài kiểm thử tự động**, **báo cáo HTML kiểm thử Android**, **32 ảnh minh chứng TC01–TC09**, **kết quả kiểm thử backend**, **dữ liệu benchmark**, **báo cáo PDF** và tài liệu hướng dẫn chạy.

Các sản phẩm chính:

- **Mã nguồn:** Android Kotlin và FastAPI Server.
- **APK:** File cài đặt để chạy thử ứng dụng.
- **Minh chứng:** 32 ảnh kiểm thử TC01–TC09.
- **Kiểm thử tự động:** 23/23 Android PASS và 26/26 Backend PASS.
- **Báo cáo:** `524H0117_524H0119.pdf`.
- **Video demo:** Được lưu trên Google Drive, liên kết ở Mục 9.
- **Hướng dẫn:** README và các lệnh cài đặt, chạy, kiểm thử.

Báo cáo PDF và video demo đã được nhóm hoàn thiện theo yêu cầu D1–D8 của đề tài. Video được lưu riêng trên Google Drive, không đưa trực tiếp vào repository do dung lượng lớn.

APK cung cấp là bản debug dùng cho thực nghiệm. Khi chạy trên máy khác, người dùng vẫn có thể build lại từ mã nguồn Android.

## 2. Cấu trúc thư mục

```text
524H0117_524H0119_GiuaKi_DeTai1/
├── Android_Source/
├── FastAPI_Server/
│   ├── main.py
│   ├── tasks.db
│   ├── requirements.txt
│   ├── requirements-dev.txt
│   ├── tests/test_api.py
│   └── README.md
├── APK/
│   └── app-debug.apk
├── MinhChung_KiemThu/
├── KetQua_KiemThu_Android/
│   └── debug/index.html
├── KET_QUA_BACKEND.txt
├── KET_QUA_BENCHMARK_API.csv
├── 524H0117_524H0119.pdf
├── README.md
└── README_CHAY_VA_TEST_CODE.md
```

**Lưu ý:** File `524H0117_524H0119.pdf` cần được đặt tại thư mục gốc GitHub để liên kết tương đối trong README hoạt động chính xác.

## 3. Công nghệ và chức năng

### Công nghệ

- **Android:** Kotlin, XML, Room, MVVM (ViewModel/Repository), Kotlin Coroutines/Flow, Retrofit và WorkManager.
- **Backend:** Python, FastAPI, SQLite và REST API.
- **Giao tiếp:** HTTP/JSON thông qua Retrofit và OkHttp.
- **Đồng bộ:** WorkManager, Push/Pull, Retry và nhật ký `SyncMetrics`.

### Chức năng

- Thêm, sửa, xóa và hoàn thành công việc khi offline.
- Lưu dữ liệu cục bộ bằng Room.
- Đồng bộ hai chiều giữa Android và FastAPI.
- Đồng bộ tự động hoặc thủ công.
- Thử lại khi máy chủ không phản hồi.
- Chống trùng dữ liệu bằng `Idempotency-Key`.
- Xử lý xung đột bằng phiên bản và thời điểm cập nhật.
- Xóa logic bằng Tombstone.

## 4. Khởi động FastAPI Server

### Windows

Mở **VS Code** tại thư mục `FastAPI_Server`, sau đó mở Terminal PowerShell và chạy:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Kiểm tra bằng trình duyệt:

- `http://127.0.0.1:8000/health` → `{"status":"ok"}`
- `http://127.0.0.1:8000/docs` → Swagger UI.
- `http://127.0.0.1:8000/tasks` → Danh sách dữ liệu trên server.

### macOS

Mở Terminal tại thư mục `FastAPI_Server` và chạy:

```bash
python3 -m venv .venv
./.venv/bin/python -m pip install -r requirements.txt
./.venv/bin/python -m uvicorn main:app --host 127.0.0.1 --port 8000
```

**Lưu ý:** FastAPI trong dự án được triển khai để demo nội bộ, chưa có xác thực người dùng và không nên mở cổng 8000 công khai trên Internet.

## 5. Chạy Android và kết nối FastAPI

### Bước 1 – Mở dự án Android

1. Mở Android Studio.
2. Chọn **Open → Android_Source**.
3. Chờ Gradle Sync hoàn tất.
4. Khởi động Android Emulator.

Thiết bị đã được sử dụng trong quá trình kiểm thử: **Medium Phone, API 37.2**.

### Bước 2 – Build ứng dụng

Mở Terminal PowerShell tại thư mục `Android_Source`:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

### Bước 3 – Thiết lập ADB Reverse

Đảm bảo FastAPI đã chạy theo hướng dẫn tại **Mục 4**.

Trong Terminal Android Studio, thực hiện:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --list
```

Nếu kết quả hiển thị mapping `tcp:8000 tcp:8000`, kết nối ADB Reverse đã được thiết lập.

### Bước 4 – Chạy ứng dụng

Nhấn **Run app** trong Android Studio hoặc cài file `APK/app-debug.apk` lên thiết bị Android phù hợp.

Sau khi ứng dụng mở:

1. Nhấn **KIỂM TRA SERVER**.
2. Nếu hiện kết nối thành công, có thể bắt đầu thử nghiệm.
3. Thêm công việc và sử dụng **ĐỒNG BỘ NGAY** để kiểm tra dữ liệu trên FastAPI.

**Quan trọng:** `RetrofitClient.kt` sử dụng:

```kotlin
BASE_URL = "http://127.0.0.1:8000/"
```

Vì vậy, Android Emulator hoặc thiết bị kết nối bằng ADB cần thiết lập ADB Reverse. Nếu khởi động lại Emulator, có thể cần chạy lại lệnh reverse.

Trên macOS, nếu Android SDK nằm ở vị trí mặc định:

```bash
~/Library/Android/sdk/platform-tools/adb reverse tcp:8000 tcp:8000
```

### Mô phỏng mất kết nối API

Để thực hiện TC02–TC05, có thể ngắt đường kết nối ADB Reverse:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --remove tcp:8000
```

Khôi phục:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
```

Cách này mô phỏng Android không thể truy cập FastAPI, **không đồng nghĩa với việc thiết bị thực sự mất Internet**.

Với TC05, có thể dừng FastAPI bằng `Ctrl + C`, sau đó khởi động lại server để quan sát cơ chế Retry.

## 6. Chạy kiểm thử tự động

### 6.1. Backend Tests

Mở Terminal tại thư mục `FastAPI_Server`:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m pytest -q tests\test_api.py
```

Kết quả đã ghi nhận:

**26/26 Backend Tests PASS.**

Xem chi tiết tại:

`KET_QUA_BACKEND.txt`

### 6.2. Benchmark API

Chạy:

```powershell
.\.venv\Scripts\python.exe scripts\benchmark_api.py --trials 5 --batch 20
```

Kết quả được lưu tại:

`KET_QUA_BENCHMARK_API.csv`

Benchmark API sử dụng database thử nghiệm riêng. Không nhầm thời gian benchmark FastAPI với thời gian đồng bộ WorkManager trên Android.

### 6.3. Android Instrumented Tests

Mở Emulator, sau đó chạy tại `Android_Source`:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Kết quả đã lưu:

- **23 tests PASS**
- **0 failures**
- **0 skipped**
- **100% successful**
- Thời gian ghi nhận: **0,560 giây**

Báo cáo tại:

`KetQua_KiemThu_Android/debug/index.html`

### 6.4. Xem log đồng bộ

Chạy trong Terminal Android Studio:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -d -s SyncMetrics:I
```

Log `SyncMetrics` hỗ trợ kiểm tra:

- Chế độ đồng bộ thủ công hoặc tự động.
- Số bản ghi đã tải lên và tải xuống.
- Số bản ghi xung đột.
- Thời gian thực hiện.
- Trạng thái lỗi hoặc thành công.

## 7. Kết quả kiểm thử TC01–TC09

| Mã | Kịch bản | Minh chứng |
|---|---|---|
| TC01 | Tạo công việc khi online | Ảnh 02–03 |
| TC02 | Tạo và sửa công việc offline | Ảnh 04–05 |
| TC03 | Khôi phục kết nối, WorkManager tự đồng bộ | Ảnh 06–08 |
| TC04 | Đóng và mở lại ứng dụng | Ảnh 09–11 |
| TC05 | Server không phản hồi, Retry | Ảnh 12–15 |
| TC06 | Đồng bộ lặp, không tạo bản ghi trùng | Ảnh 16–18 |
| TC07 | Xử lý xung đột Server Wins và Android Wins | Ảnh 19–25 |
| TC08 | Xóa công việc offline bằng Tombstone | Ảnh 26–29 |
| TC09 | Đồng bộ hàng loạt 20 công việc | Ảnh 30–32 |

Toàn bộ ảnh được lưu tại:

`MinhChung_KiemThu/`

Ảnh `01_AndroidTest_23PASS.png` minh chứng kết quả Android Instrumented Tests.

### Một số kết quả tiêu biểu

**TC03 – Đồng bộ tự động**

- WorkManager ghi nhận `Uploaded=1`.
- Thời gian thực hiện của lượt kiểm thử: `2.170 seconds`.

**TC07 – Xử lý xung đột**

- Có hai trường hợp Server Wins và Android Wins.
- Xung đột được mô phỏng bằng một Android Emulator và Swagger UI trên PC.
- Chưa kiểm thử đồng thời bằng hai điện thoại Android vật lý.

**TC09 – Đồng bộ hàng loạt**

- Số công việc đồng bộ: **20/20**.
- `Uploaded=20`.
- `LocalCompleted=20/20`.
- `Conflicts=0`.
- Thời gian ghi nhận: **2,165 giây**.
- Thông lượng ước tính riêng lượt đo: **9,24 công việc/giây**.
- Không ghi nhận bản ghi trùng trong lượt kiểm thử.

Các kết quả trên phản ánh môi trường và các lượt thực nghiệm đã ghi nhận, không phải cam kết hiệu năng trên mọi thiết bị hoặc điều kiện mạng.

## 8. SQLite mẫu, giới hạn và lưu ý

### SQLite mẫu

File `FastAPI_Server/tasks.db` trong bản bàn giao ngày 04/11/2026 đã được kiểm tra bằng SQLite `PRAGMA integrity_check`, kết quả `ok`.

Tại thời điểm bàn giao, database có:

- **87 bản ghi**
- **84 bản ghi chưa xóa**
- **3 bản ghi xóa logic**

Đây là dữ liệu demo sau kiểm thử, không phải database trống. Số lượng có thể thay đổi nếu tiếp tục thực hiện các thao tác demo.

Nếu muốn kiểm thử từ database trống:

1. Dừng FastAPI.
2. Sao lưu `tasks.db`.
3. Đổi tên database gốc.
4. Khởi động lại FastAPI để tạo database mới.

Không xóa dữ liệu demo nếu chưa có bản sao lưu.

### Giới hạn hệ thống

- WorkManager hoạt động theo lịch của Android, không bảo đảm chạy ngay lập tức khi kết nối được khôi phục.
- Tác vụ định kỳ có chu kỳ tối thiểu 15 phút nhưng không bảo đảm thực thi đúng thời điểm đó.
- Xử lý xung đột theo `updatedAt` có thể chịu ảnh hưởng nếu đồng hồ của các bên sai lệch.
- Backend demo chưa có xác thực và chưa triển khai HTTPS công khai.
- APK được cung cấp là debug APK, dành cho cài đặt và thực nghiệm.
- Chưa có kiểm thử đồng thời trên hai điện thoại Android vật lý.
- Cấu hình timeout đã có trong source, nhưng chưa có đầy đủ phép đo thực tế cho mọi tình huống mạng chậm.

**Lưu ý bảo mật:** Khi repository ở chế độ Public, cần bảo đảm `tasks.db`, ảnh minh chứng, các file cấu hình và lịch sử Git không chứa dữ liệu nhạy cảm.

## 9. Báo cáo PDF, video demo và sản phẩm nộp

Nhóm đã hoàn thành các sản phẩm theo yêu cầu D1–D8 của đề tài.

### 9.1. Báo cáo tiểu luận PDF

**Tên file:** `524H0117_524H0119.pdf`

[Xem báo cáo PDF](524H0117_524H0119.pdf)

Báo cáo gồm 38 trang, trình bày cơ sở lý thuyết, phân tích thiết kế, kiến trúc hệ thống, đồng bộ hai chiều, xử lý xung đột, kiểm thử và đánh giá thực nghiệm.

### 9.2. Video demo

**Video trình diễn các trường hợp TC01–TC09:**

[Xem video demo trên Google Drive](https://drive.google.com/file/d/1ZpKOfXn_und923KkA6hpTPUuKTHu9Mdq/view?usp=drive_link)

Video bao gồm:

- Khởi động FastAPI và kết nối Android Emulator.
- Thực hiện thao tác online/offline.
- Kiểm tra đồng bộ WorkManager và Retry.
- Kiểm tra chống trùng dữ liệu.
- Xử lý xung đột Server Wins và Android Wins.
- Xóa offline bằng Tombstone.
- Đồng bộ hàng loạt 20 công việc.
- Giới thiệu kết quả kiểm thử và GitHub.

### 9.3. Đối chiếu D1–D8

| Yêu cầu | Sản phẩm / minh chứng |
|---|---|
| D1 | Báo cáo lý thuyết PDF |
| D2 | Phân tích thiết kế và sơ đồ trong báo cáo |
| D3 | Source Android trên GitHub |
| D4 | FastAPI Server, REST API và tài liệu API |
| D5 | Cơ chế đồng bộ và xử lý xung đột |
| D6 | TC01–TC09, 32 ảnh minh chứng và kết quả kiểm thử |
| D7 | Kết quả thực nghiệm và benchmark |
| D8 | APK, video demo và README hướng dẫn |

### 9.4. GitHub Repository

[Mã nguồn dự án](https://github.com/nguyengiaphat695-coder/524H0117_524H0119_GiuaKi_DeTai1)

---

**Ghi chú:** Repository cung cấp mã nguồn Android Kotlin, FastAPI Server, APK debug, báo cáo PDF, kết quả kiểm thử, ảnh minh chứng và hướng dẫn cài đặt. Video demo được lưu trên Google Drive để thuận tiện cho việc xem trực tuyến.

Để đánh giá hoặc tái hiện hệ thống, người đọc có thể sử dụng mã nguồn và các hướng dẫn ở trên, đồng thời tham khảo `FastAPI_Server/README.md` để biết chi tiết API.
