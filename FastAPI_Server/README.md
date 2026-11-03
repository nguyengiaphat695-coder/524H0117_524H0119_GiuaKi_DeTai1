# FastAPI Server — Offline-First Task Manager

Backend Python + FastAPI + SQLite của ứng dụng Android Kotlin Offline-First.

> Đây là API demo học tập, chưa có xác thực người dùng. Chỉ chạy nội bộ trên `127.0.0.1`, không triển khai trực tiếp lên Internet.

## Chạy trên Windows PowerShell

Mở Terminal **ngay tại thư mục `FastAPI_Server`** (sau khi giải nén bộ bàn giao) rồi chạy:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

## Chạy trên macOS

Mở Terminal tại thư mục `FastAPI_Server` rồi chạy:

```bash
python3 -m venv .venv
./.venv/bin/python -m pip install -r requirements.txt
./.venv/bin/python -m uvicorn main:app --host 127.0.0.1 --port 8000
```

Kiểm tra `http://127.0.0.1:8000/health` (trả về `{"status":"ok"}`) và Swagger UI tại `http://127.0.0.1:8000/docs`.

## Kết nối từ Android Emulator

Android source hiện dùng `BASE_URL = "http://127.0.0.1:8000/"` trong `RetrofitClient.kt`. Vì vậy cần bật **ADB reverse** sau khi Emulator chạy:

**Windows (PowerShell):**

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
```

**macOS (Terminal, SDK ở đường dẫn mặc định):**

```bash
~/Library/Android/sdk/platform-tools/adb reverse tcp:8000 tcp:8000
```

Nếu Emulator khởi động lại hoặc không kết nối được, chạy lại ADB reverse. `10.0.2.2` là cách kết nối khác thường dùng trong Emulator nhưng **không phải** URL mà project Android hiện đang cấu hình.

## Endpoints

- `GET /health`: kiểm tra trạng thái server
- `GET /tasks`: lấy danh sách công việc (có cả bản ghi xóa logic)
- `GET /tasks/{task_id}`: lấy một công việc
- `POST /tasks`: tạo công việc với ID từ Android
- `PUT /tasks/{task_id}`: cập nhật; dùng `expectedVersion` để phát hiện xung đột
- `DELETE /tasks/{task_id}?expectedVersion=...`: xóa logic

`POST`, `PUT` và `DELETE` hỗ trợ header `Idempotency-Key`. Gửi lại cùng yêu cầu với cùng khóa sẽ không tạo thêm thao tác; dùng lại khóa cho yêu cầu khác có thể trả HTTP 409.

## Database

`main.py` tạo bảng SQLite tự động khi khởi động. File `tasks.db` kèm theo chứa **dữ liệu demo đã kiểm thử**. Nếu cần test trên database trống, trước tiên sao lưu và đổi tên `tasks.db` khi server đã dừng; khởi động lại server để tạo bản mới. Muốn khôi phục dữ liệu demo, dừng server rồi chép lại bản đã sao lưu.

Xem README_CHAY_VA_TEST_CODE.md o thu muc goc de build Android va chay test.

## Chạy kiểm thử API tự động (bổ sung cho bài giữa kỳ)

Bộ test được lưu tại `tests/test_api.py`; mỗi test tạo file SQLite riêng trong thư mục tạm do pytest quản lý, **không ghi vào `tasks.db` demo**.

Windows PowerShell trong thư mục `FastAPI_Server`:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m pytest -q tests\test_api.py
```

Kết quả ở môi trường biên soạn: `26 passed (kiem tra tren moi truong tao ma nguon)`. Các kiểm thử này kiểm tra backend, không thay thế các kịch bản Android TC01–TC09.
