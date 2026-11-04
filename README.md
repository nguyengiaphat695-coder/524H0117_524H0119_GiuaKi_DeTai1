# OFFLINE-FIRST TASK MANAGER – ĐỀ TÀI 01

**Nhóm:** 524H0117 – 524H0119  
**Môn học:** Phát triển ứng dụng di động  
**Đề tài:** Xây dựng ứng dụng Android hỗ trợ hoạt động ngoại tuyến và đồng bộ dữ liệu tự động.

## 1. Giới thiệu dự án

Offline-First Task Manager là ứng dụng quản lý công việc cá nhân trên Android, được phát triển bằng Kotlin, Room, MVVM, Retrofit, Coroutines/Flow và WorkManager. Máy chủ sử dụng FastAPI kết hợp SQLite.

Ứng dụng hỗ trợ:

- Thêm, sửa, xóa và hoàn thành công việc khi offline.
- Đồng bộ dữ liệu hai chiều giữa Android và FastAPI.
- Đồng bộ tự động bằng WorkManager và đồng bộ thủ công.
- Tự động thử lại (Retry) khi kết nối hoặc máy chủ gặp lỗi.
- Xóa logic bằng cơ chế Tombstone.
- Hạn chế tạo bản ghi trùng bằng Idempotency-Key.
- Phát hiện và xử lý xung đột dữ liệu theo phiên bản và thời điểm cập nhật.

## 2. Mã nguồn và sản phẩm

| Thư mục / Tệp | Nội dung |
|---|---|
| `Android_Source/` | Mã nguồn Android Kotlin và các bài kiểm thử |
| `FastAPI_Server/` | REST API, SQLite và kiểm thử Backend |
| `APK/app-debug.apk` | APK debug để cài đặt và chạy thử |
| `MinhChung_KiemThu/` | 32 ảnh minh chứng cho TC01–TC09 |
| `KetQua_KiemThu_Android/debug/index.html` | Kết quả kiểm thử Android: 23/23 PASS |
| `KET_QUA_BACKEND.txt` | Kết quả kiểm thử Backend: 26/26 PASS |
| `KET_QUA_BENCHMARK_API.csv` | Dữ liệu đo hiệu năng API |
| `README_CHAY_VA_TEST_CODE.md` | Hướng dẫn cài đặt, chạy và kiểm thử |
| `524H0117_524H0119.pdf` | Báo cáo tiểu luận giữa kỳ |

## 3. Hướng dẫn cài đặt và kiểm thử

Xem tài liệu [README_CHAY_VA_TEST_CODE.md](README_CHAY_VA_TEST_CODE.md) để biết cách:

- Build và chạy ứng dụng Android.
- Khởi động FastAPI Server.
- Kết nối Android Emulator với FastAPI bằng ADB Reverse.
- Thực hiện các trường hợp kiểm thử TC01–TC09.
- Kiểm tra log WorkManager và kết quả đồng bộ.
- Xem kết quả kiểm thử tự động và benchmark API.

## 4. Video demo

**Video trình diễn ứng dụng và các trường hợp kiểm thử TC01–TC09:**

[**XEM VIDEO DEMO TRÊN GOOGLE DRIVE**](https://drive.google.com/file/d/1ZpKOfXn_und923KkA6hpTPUuKTHu9Mdq/view?usp=drive_link)

Video trình bày quá trình khởi động hệ thống, thao tác online/offline, đồng bộ tự động, Retry, xử lý xung đột, xóa logic và kiểm thử đồng bộ nhiều công việc.

## 5. Báo cáo tiểu luận giữa kỳ

**Báo cáo PDF chính thức của nhóm:**

[**XEM BÁO CÁO PDF**](524H0117_524H0119.pdf)

Báo cáo gồm 38 trang, trình bày các nội dung theo yêu cầu D1–D8:

- **D1:** Cơ sở lý thuyết Offline-First, MVVM, Room, Retrofit, Coroutines/Flow và WorkManager.
- **D2:** Phân tích yêu cầu, thiết kế kiến trúc hệ thống, Use Case, cơ sở dữ liệu và luồng đồng bộ.
- **D3:** Mã nguồn ứng dụng Android Kotlin.
- **D4:** FastAPI Server, SQLite và tài liệu API.
- **D5:** Cơ chế đồng bộ hai chiều, Retry, Tombstone, Idempotency và xử lý xung đột.
- **D6:** Các trường hợp kiểm thử TC01–TC09 cùng kết quả và ảnh minh chứng.
- **D7:** Báo cáo thực nghiệm, đánh giá hiệu năng và những hạn chế của hệ thống.
- **D8:** APK, video demo và hướng dẫn chạy ứng dụng.

## 6. Kết quả kiểm thử

| Hạng mục | Kết quả |
|---|---|
| Android Instrumented Tests | 23/23 PASS |
| FastAPI Backend Tests | 26/26 PASS |
| Minh chứng kiểm thử | 32 ảnh |
| Các kịch bản thực nghiệm | TC01–TC09 |
| Đồng bộ hàng loạt TC09 | 20/20 công việc |
| Thời gian đồng bộ TC09 | 2,165 giây trong một lượt đo |

Các số liệu trên phản ánh kết quả kiểm thử trong môi trường thực nghiệm của nhóm, không phải cam kết hiệu năng trên mọi thiết bị hoặc điều kiện mạng.

## 7. GitHub Repository

[Mã nguồn dự án trên GitHub](https://github.com/nguyengiaphat695-coder/524H0117_524H0119_GiuaKi_DeTai1)

---

**Ghi chú:** Repository cung cấp mã nguồn Android, FastAPI Server, APK, báo cáo PDF, kết quả kiểm thử, ảnh minh chứng và tài liệu hướng dẫn. Video demo được lưu trên Google Drive thông qua đường dẫn tại Mục 4.
