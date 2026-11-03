# OFFLINE-FIRST TASK MANAGER – ĐỀ TÀI 01 (GIỮA KỲ)

**Nhóm:** 524H0117 – 524H0119  
**Đề tài:** Xây dựng ứng dụng Android hỗ trợ hoạt động ngoại tuyến và đồng bộ dữ liệu tự động.  
**Ứng dụng minh họa:** Offline-First Task Manager.  
**Repository GitHub:** https://github.com/nguyengiaphat695-coder/524H0117_524H0119_GiuaKi_DeTai1

## 1. Giới thiệu và phạm vi bộ bàn giao

Bộ bàn giao này gồm **mã nguồn Android Kotlin**, **FastAPI Server**, **SQLite mẫu**, **APK debug**, **bài kiểm thử tự động**, **báo cáo HTML kiểm thử Android**, **32 ảnh minh chứng TC01–TC09**, **kết quả backend**, **dữ liệu benchmark** và tài liệu hướng dẫn chạy.

**Chưa bao gồm:** video demo và báo cáo lý thuyết/thiết kế Word/PDF. Đây là các sản phẩm riêng cần bổ sung khi nộp đầy đủ D1–D8. Việc có sẵn APK debug không loại bỏ nhu cầu build lại khi chạy trên máy khác.

## 2. Cấu trúc thư mục

```text
524H0117_524H0119_GiuaKi_DeTai1/
├── Android_Source/                  # Project Android Studio Kotlin
├── FastAPI_Server/                 # FastAPI, SQLite, test, benchmark
│   ├── main.py
│   ├── tasks.db                    # Dữ liệu demo sau kiểm thử
│   ├── requirements.txt
│   ├── requirements-dev.txt
│   ├── tests/test_api.py
│   └── README.md
├── APK/app-debug.apk               # APK đã build thành công
├── MinhChung_KiemThu/              # 32 ảnh được đánh số 01–32
├── KetQua_KiemThu_Android/         # Báo cáo Android instrumented tests
│   └── debug/index.html
├── KET_QUA_BACKEND.txt              # Kết quả pytest backend
├── KET_QUA_BENCHMARK_API.csv       # Benchmark API 5 lần chạy
├── README.md                       # Giới thiệu ngắn trên GitHub
└── README_CHAY_VA_TEST_CODE.md     # Tài liệu chi tiết này
```

## 3. Công nghệ và chức năng

- **Android:** Kotlin, XML, Room, MVVM (ViewModel/Repository), Kotlin Coroutines/Flow, Retrofit và WorkManager.
- **Backend:** Python, FastAPI, SQLite, API REST (GET/POST/PUT/DELETE).
- **Offline-First:** xem và cập nhật tác vụ khi mất kết nối; lưu thay đổi cục bộ trong Room.
- **Đồng bộ:** gửi thay đổi (push), tải dữ liệu (pull), đồng bộ tay/tự động, retry và log `SyncMetrics`.
- **Tránh trùng và xung đột:** ID ổn định, `Idempotency-Key`, `expectedVersion`/HTTP 409, chính sách chọn bản cập nhật theo `updatedAt` và tombstone cho xóa logic.

## 4. Khởi động FastAPI trên Windows

Mở **VS Code** tại thư mục `FastAPI_Server`, mở PowerShell rồi chạy:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Kiểm tra:

- `http://127.0.0.1:8000/health` → `{"status":"ok"}`
- `http://127.0.0.1:8000/docs` → giao diện Swagger
- `http://127.0.0.1:8000/tasks` → danh sách dữ liệu mẫu trên server

**macOS (Terminal tại `FastAPI_Server`):**

```bash
python3 -m venv .venv
./.venv/bin/python -m pip install -r requirements.txt
./.venv/bin/python -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Đây là server **demo nội bộ**, không có xác thực người dùng; không mở port 8000 công khai trên Internet.

## 5. Chạy Android và kết nối FastAPI

1. Mở **Android Studio → Open → `Android_Source`**. Chờ Gradle Sync hoàn tất.
2. Khởi động Android Emulator (bản đã kiểm thử: Medium Phone, API 37.2).
3. Trong Terminal PowerShell tại `Android_Source`, chạy:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

4. Để FastAPI chạy ở bước 4. Thiết lập ADB Reverse:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --list
```

5. Nhấn **Run app** trong Android Studio hoặc cài `APK/app-debug.apk` lên thiết bị thích hợp.

**Quan trọng:** `RetrofitClient.kt` dùng `BASE_URL = "http://127.0.0.1:8000/"`, vì vậy Emulator/thiết bị kết nối bằng ADB cần thiết lập ADB Reverse. Nếu khởi động lại Emulator, có thể phải chạy lệnh reverse lần nữa.

**macOS:** sử dụng `~/Library/Android/sdk/platform-tools/adb reverse tcp:8000 tcp:8000` nếu SDK cài tại vị trí mặc định.

### Mô phỏng mất kết nối cho TC02–TC05

Với môi trường đang dùng ADB Reverse, chế độ máy bay có thể **không ngắt được** đường kết nối reverse. Có thể mô phỏng mất kết nối bằng cách xóa mapping:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --remove tcp:8000
```

Khôi phục bằng lệnh `adb reverse tcp:8000 tcp:8000` bên trên. Với TC05 cũng có thể dừng FastAPI (`Ctrl+C`) rồi khởi động lại để quan sát WorkManager retry.

## 6. Chạy kiểm thử tự động

**Backend** – Terminal tại `FastAPI_Server`:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m pytest -q tests\test_api.py
.\.venv\Scripts\python.exe scripts\benchmark_api.py --trials 5 --batch 20
```

Backend tests và benchmark sử dụng database thử nghiệm riêng; không dùng để thay thế kiểm thử Android thực tế. File `KET_QUA_BACKEND.txt` ghi nhận **26/26 test backend PASS**. File `KET_QUA_BENCHMARK_API.csv` lưu năm lượt benchmark API; không nhầm các tốc độ này với thời gian WorkManager trên Android.

**Android** – Terminal tại `Android_Source`, sau khi mở Emulator:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Báo cáo đã lưu tại `KetQua_KiemThu_Android/debug/index.html`: **23 tests, 0 failures, 0 skipped, 100% successful (0,560 giây)**.

Để xem log đồng bộ tự động:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -d -s SyncMetrics:I
```

## 7. Kết quả kiểm thử TC01–TC09 trên máy nhóm

| Mã | Kịch bản | Minh chứng trong `MinhChung_KiemThu` |
|---|---|---|
| TC01 | Tạo online, dữ liệu xuất hiện ở Android và server | `02_`, `03_` |
| TC02 | Tạo/sửa offline, Room vẫn lưu | `04_`, `05_` |
| TC03 | Khôi phục kết nối, WorkManager tự đồng bộ | `06_`–`08_` |
| TC04 | Đóng/mở ứng dụng, dữ liệu bền vững và đồng bộ nền | `09_`–`11_` |
| TC05 | Server ngừng hoạt động, thất bại rồi tự retry thành công | `12_`–`15_` |
| TC06 | Đồng bộ lặp, không có bản ghi trùng | `16_`–`18_` |
| TC07 | Phát hiện và xử lý xung đột Server Wins / Android Wins | `19_`–`25_` |
| TC08 | Xóa offline, server đánh dấu tombstone, không tải lại | `26_`–`29_` |
| TC09 | Đồng bộ hàng loạt 20 task, không trùng ID/tên | `30_`–`32_` |

**Ảnh `01_AndroidTest_23PASS.png`** là minh chứng tổng hợp báo cáo Android instrumented tests.

- TC03: log tự động ghi nhận `Uploaded=1`, `Duration=2.170 seconds`.
- TC09: log WorkManager thực tế ghi nhận **Uploaded=20**, **LocalCompleted=20/20**, **Duration=2.165 seconds**, **Conflicts=0**; kiểm tra 20 tác vụ không trùng ID/tên. Tốc độ quy đổi riêng lượt đó khoảng **9,24 tác vụ/giây**.
- TC07: tình huống hai phía thay đổi được **mô phỏng bằng một Android Emulator và Swagger trên PC**, chưa phải hai điện thoại vật lý cùng hoạt động.
- TC05: đã thử thực tế trường hợp server dừng rồi retry; riêng tình huống phản hồi HTTP quá chậm được đề cập trong bộ Android instrumented tests nhưng chưa có video minh họa thủ công.

Không diễn giải những giới hạn trên thành bằng chứng của kịch bản chưa chạy thực tế.

## 8. SQLite mẫu, giới hạn và lưu ý

- File `FastAPI_Server/tasks.db` trong bản bàn giao ngày 04/11/2026 được kiểm tra bằng SQLite `PRAGMA integrity_check` cho kết quả `ok`, có **87 bản ghi**: 84 chưa xóa và 3 bản ghi xóa logic. Đây là dữ liệu demo sau kiểm thử, không phải database trống.
- Muốn test với database trống: **dừng FastAPI**, sao lưu `tasks.db`, đổi tên file gốc và khởi động lại server để tạo database mới. Không xóa dữ liệu demo nếu chưa có bản sao.
- `WorkManager` chạy theo lịch do Android quyết định; chu kỳ tối thiểu 15 phút không đồng nghĩa bảo đảm chạy đúng sau 15 phút.
- Chọn dữ liệu theo timestamp (`updatedAt`) có thể cho kết quả không mong muốn khi đồng hồ các bên sai lệch; xóa logic cần được bảo toàn qua các vòng đồng bộ.
- Bản APK kèm theo là **debug APK**, dùng để demo/cài thử, không phải bản release để phát hành.
- Trước khi chuyển GitHub sang **Public**, phải rà soát `tasks.db`, ảnh chụp và lịch sử Git để bảo đảm không công khai dữ liệu nhạy cảm.

## 9. Hạng mục còn cần hoàn thiện ngoài bộ code

Theo đề gốc: D1 báo cáo lý thuyết PDF; D2 tài liệu thiết kế/sơ đồ; D3 Git repository và hướng dẫn build (đã có source/link); D4 server/API; D5 sơ đồ thuật toán đồng bộ; D6 test cases và minh chứng; D7 đánh giá và số liệu thực nghiệm; D8 **APK + video demo + README**.

**Video demo và báo cáo PDF cuối cùng chưa được đính kèm trong bộ ZIP code này.** Xem thêm `FastAPI_Server/README.md` để biết chi tiết API.
