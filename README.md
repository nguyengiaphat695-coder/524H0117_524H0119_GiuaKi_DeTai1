# Offline-First Task Manager – Đề tài 01

**Nhóm:** 524H0117 – 524H0119  
**Tiểu luận giữa kỳ:** Xây dựng ứng dụng Android hỗ trợ hoạt động ngoại tuyến và đồng bộ dữ liệu tự động.

## 1. Giới thiệu dự án

Offline-First Task Manager là ứng dụng quản lý công việc trên Android, được phát triển bằng Kotlin, Room, MVVM, Retrofit, Coroutines/Flow và WorkManager. Máy chủ sử dụng FastAPI kết hợp SQLite.

Ứng dụng hỗ trợ:
- Thêm, sửa, xóa và hoàn thành công việc khi offline.
- Đồng bộ dữ liệu hai chiều giữa Android và FastAPI.
- Tự động đồng bộ bằng WorkManager.
- Retry khi kết nối hoặc máy chủ gặp lỗi.
- Xóa logic bằng Tombstone.
- Chống trùng dữ liệu bằng Idempotency.
- Phát hiện và xử lý xung đột dữ liệu.

## 2. Mã nguồn và sản phẩm

| Thư mục / Tệp | Nội dung |
|---|---|
| `Android_Source/` | Mã nguồn Android và kiểm thử |
| `FastAPI_Server/` | REST API, SQLite và kiểm thử Backend |
| `APK/app-debug.apk` | File APK cài đặt ứng dụng |
| `MinhChung_KiemThu/` | 32 ảnh minh chứng TC01–TC09 |
| `KetQua_KiemThu_Android/debug/index.html` | Kết quả Android: 23/23 PASS |
| `KET_QUA_BACKEND.txt` | Kết quả Backend: 26/26 PASS |
| `KET_QUA_BENCHMARK_API.csv` | Kết quả đo hiệu năng API |

## 3. Hướng dẫn cài đặt và kiểm thử

Xem tài liệu [README_CHAY_VA_TEST_CODE.md](README_CHAY_VA_TEST_CODE.md) để biết cách:

- Build và chạy ứng dụng Android.
- Khởi động FastAPI Server.
- Kết nối Android Emulator bằng ADB Reverse.
- Thực hiện các trường hợp kiểm thử TC01–TC09.
- Xem log đồng bộ và kết quả kiểm thử.

## 4. Video demo

**Video trình diễn ứng dụng và các trường hợp kiểm thử TC01–TC09:**

[**Xem video demo trên Google Drive**](https://drive.google.com/file/d/1ZpKOfXn_und923KkA6hpTPUuKTHu9Mdq/view?usp=drive_link)

Video bao gồm quá trình vận hành ứng dụng, kiểm thử offline, đồng bộ dữ liệu, xử lý xung đột và đánh giá kết quả.

## 5. Báo cáo tiểu luận

Báo cáo PDF trình bày cơ sở lý thuyết, phân tích thiết kế, kiến trúc hệ thống, cơ chế đồng bộ, kết quả kiểm thử và đánh giá thực nghiệm theo yêu cầu D1–D8.

**Trạng thái:** Báo cáo đang được hoàn thiện và sẽ bổ sung đường dẫn sau.

## 6. GitHub Repository

[Mã nguồn dự án trên GitHub](https://github.com/nguyengiaphat695-coder/524H0117_524H0119_GiuaKi_DeTai1)

---

**Ghi chú:** Repository cung cấp mã nguồn, APK, kết quả kiểm thử và hướng dẫn chạy chương trình. Video demo và báo cáo PDF được cung cấp thông qua các đường dẫn tương ứng để phục vụ việc đánh giá đề tài.
