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
- **Đồng bộ:** WorkManager, Push/Pull, Retry và nhật ký
