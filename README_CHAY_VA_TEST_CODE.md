# OFFLINE-FIRST TASK MANAGER - SOURCE CODE DA SUA

Ban nay **chi chua code Android Kotlin va FastAPI, bo test va du lieu demo SQLite**. Khong kem slide, kich ban thuyet trinh, bao cao Word, video, hoac APK cu. Apk can duoc build moi tu Android_Source.

## A. Thanh phan

- `Android_Source/`: project Android Studio Kotlin XML, Room, MVVM, Retrofit, WorkManager.
- `FastAPI_Server/`: `main.py`, `requirements.txt`, `tasks.db` demo, API tests, script benchmark.
- `Android_Source/setup_adb_reverse.ps1`: chuyen tiep cong 8000 cho tat ca emulator hoac thiet bi Android dang ket noi bang ADB.
- `KET_QUA_BACKEND.txt`: ket qua test da chay voi database tam.

## B. Chay FastAPI tren Windows

1. Mo VS Code -> File -> Open Folder -> `FastAPI_Server`.
2. Terminal -> New Terminal, chay:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-dev.txt
.\.venv\Scripts\python.exe -m uvicorn main:app --host 127.0.0.1 --port 8000
```

3. Vao `http://127.0.0.1:8000/health` de xem `{"status":"ok"}` va `http://127.0.0.1:8000/docs` de xem API.
4. De chay backend test (dung Terminal moi khi server dang chay):

```powershell
.\.venv\Scripts\python.exe -m pytest -q tests\test_api.py
.\.venv\Scripts\python.exe scripts\benchmark_api.py --trials 5 --batch 20
```

Cac kiem thu pytest va benchmark su dung database rieng trong thu muc tam, KHONG sua `tasks.db` demo.

## C. Chay FastAPI tren macOS

```bash
cd FastAPI_Server
python3 -m venv .venv
./.venv/bin/python -m pip install -r requirements-dev.txt
./.venv/bin/python -m uvicorn main:app --host 127.0.0.1 --port 8000
```

## D. Build Android tren Windows

1. Android Studio -> Open -> chon thu muc `Android_Source`. Cho Gradle Sync va SDK duoc cai dat.
2. Terminal trong Android Studio, chay:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug
```

3. Neu thay `BUILD SUCCESSFUL`, APK moi nam tai `Android_Source\app\build\outputs\apk\debug\app-debug.apk`.
4. Mo Emulator va de FastAPI dang chay. Trong Terminal Android Studio:

```powershell
.\setup_adb_reverse.ps1
```

Neu PowerShell chan script, khong can doi policy; chay truc tiep:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
```

5. Trong Android Studio, nhan Run app. App hien thi task Room khi offline va co nut dong bo thu cong.
6. De chay Android Instrumented Test, khoi dong Emulator truoc, roi chay:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

7. Xem log tu dong:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s SyncMetrics:I
```

## E. Hai thiet bi Android cung ket noi backend

Co the khoi dong hai emulator; chay `setup_adb_reverse.ps1` de thiet lap port reverse rieng cho moi serial ADB. Hai emulator dung chung FastAPI tren may tinh. Moi emulator co Room database doc lap. Neu chi dung mot emulator va Swagger tren PC thi day la mo phong hai phia, khong phai hai Android thuc.

## F. Test case cap code

- TC01: Tao online -> Room + server; kiem tra Swagger `GET /tasks`.
- TC02: Dung FastAPI / tat mang -> them, sua, xoa, hoan thanh; Room van thay task.
- TC03: Bat lai mang -> WorkManager auto sync; kiem tra log va server.
- TC04: Dong va mo lai app -> Room giu task; test `persistentRoomDatabaseSurvivesCloseAndReopen`.
- TC05: Server loi / timeout -> trang thai FAILED va retry; test `timeoutFailureLeavesTaskForNextRetry`.
- TC06: Gui lap POST/PUT/DELETE -> Idempotency-Key; pytest anti duplicate.
- TC07: Hai ban sao Room xung dot -> test `twoIndependentRoomDevicesDetectAndResolveVersionConflict`.
- TC08: Tombstone -> khong hien lai sau Pull; test Server tombstone va xoa local chua upload.
- TC09: 20 tasks × 5 luot backend; du lieu khong trung; Android SyncMetrics co dem `LocalCompleted`.

## G. Thong tin quan trong

- `BASE_URL` cua Android: `http://127.0.0.1:8000/`, phai ADB reverse tu emulator/USB device.
- WorkManager periodic toi thieu 15 phut, do he dieu hanh lap lich, KHONG cam ket chay dung 15 phut.
- LWW dua tren updatedAt nen neu dong ho hai thiet bi lech nhau, co the co ket qua chon khong mong muon. Server tombstone duoc uu tien hon local edit.
- Server la demo noi bo, khong co user authentication hay HTTPS. Khong public port 8000 len Internet.
- Ket qua backend PASS da duoc xac minh tai day; Android Instrumented Tests da viet nhung CHUA the chay trong moi truong tao ZIP do thieu Android SDK/Emulator. Phai chay tren may Windows cua ban de xac nhan build va test.
- Demo SQLite co 59 task cua ban va da duoc giu nguyen. Khi muon test sach: dung server, sao luu doi ten `tasks.db`; khi khoi dong lai `main.py` se tao db moi.
