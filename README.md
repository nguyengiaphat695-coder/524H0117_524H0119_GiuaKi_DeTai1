# Offline-First Task Manager – Đề tài 01

**Nhóm:** 524H0117 – 524H0119  
**Tiểu luận giữa kỳ:** Xây dựng ứng dụng Android hỗ trợ hoạt động ngoại tuyến và đồng bộ dữ liệu tự động.

Ứng dụng quản lý công việc được xây dựng bằng Kotlin, Room, MVVM, Retrofit, Coroutines/Flow và WorkManager; máy chủ dùng FastAPI + SQLite. Hỗ trợ CRUD offline, đồng bộ hai chiều, retry, tombstone, idempotency và xử lý xung đột phiên bản.

**Mã nguồn và sản phẩm:**
- `Android_Source/` – Android project và Android instrumented tests.
- `FastAPI_Server/` – REST API, SQLite demo, backend automated tests.
- `APK/app-debug.apk` – APK debug đã build.
- `MinhChung_KiemThu/` – 32 ảnh kiểm thử TC01–TC09.
- `KetQua_KiemThu_Android/debug/index.html` – Android tests: 23/23 PASS.
- `KET_QUA_BACKEND.txt` – Backend tests: 26/26 PASS.
- `KET_QUA_BENCHMARK_API.csv` – Số liệu benchmark API.

**Hướng dẫn build, chạy server, kết nối ADB Reverse, kiểm thử và mô tả các giới hạn:** xem [README_CHAY_VA_TEST_CODE.md](README_CHAY_VA_TEST_CODE.md).

**GitHub:** https://github.com/nguyengiaphat695-coder/524H0117_524H0119_GiuaKi_DeTai1

> Đây là bộ bàn giao phần code. Video demo và báo cáo lý thuyết/thiết kế cần nộp riêng theo yêu cầu D1–D8 của đề.
