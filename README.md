# Quảng Cáo Quốc Đăng – Android App

Ứng dụng Android dạng WebView hiển thị website **https://quangcaoktd.com/**,
được build bằng Android Studio, sẵn sàng đăng lên CH Play (Google Play Console).

## Thông tin app

| Mục | Giá trị |
|---|---|
| Package / Application ID | `com.quangcaoktd.app` |
| Tên hiển thị | Quảng Cáo Quốc Đăng |
| Trang chủ tải sẵn | https://quangcaoktd.com/ |
| minSdkVersion | 21 (Android 5.0) |
| targetSdkVersion / compileSdkVersion | 36 |
| versionCode / versionName | 1 / 1.0.0 |
| Quyền yêu cầu | **Chỉ `INTERNET`** — không camera, không vị trí, không lưu trữ |

## Tính năng đã xử lý

- **WebView tối ưu**: bật JavaScript, DOM storage, zoom, cache — để form
  "Yêu cầu tư vấn – Báo giá" và toàn bộ nội dung site hoạt động bình thường.
- **Header sóng (WavyHeaderView)**: che vùng camera/loa/pin bằng dải màu
  `#0277A4`, viền dưới bo sóng/giọt nước y hệt mẫu gốc — chỉ đổi màu.
- **Điều hướng thông minh** (`MainActivity#handleUrlRouting`):
  - Link cùng domain `quangcaoktd.com` (kể cả subdomain) → mở ngay trong app.
  - `tel:`, `sms:`, `mailto:`, Zalo (`zalo.me`), Facebook/Messenger, Google Maps,
    liên kết `target="_blank"` → tự mở bằng app tương ứng đã cài trên máy
    (hoặc trình duyệt ngoài nếu không có app phù hợp).
  - Không có app phù hợp → hiện Toast báo lỗi thay vì crash.
- **Chỉ tải qua HTTPS**: `network_security_config.xml` chặn cleartext,
  chỉ cho phép `quangcaoktd.com` (và subdomain).
- **Trạng thái mạng**: thanh tiến trình tải trang, kéo để làm mới
  (pull-to-refresh), và màn hình báo "Không có kết nối internet" kèm nút
  "Thử lại" khi trang chính load lỗi.
- **SSL an toàn**: không bỏ qua lỗi chứng chỉ SSL (`onReceivedSslError` luôn
  `cancel()`), tránh lỗ hổng bảo mật man-in-the-middle.
- **Nút Back**: quay lại trang trước trong WebView nếu có lịch sử, thoát app
  nếu không còn.
- **Khôi phục trạng thái**: lưu/khôi phục lịch sử WebView khi xoay màn hình
  hoặc app bị hệ thống thu hồi tạm thời.

## Icon ứng dụng

Icon được tạo từ file `logo-qc-ok-removebg.png` bạn cung cấp, tự động resize
cho mọi mật độ màn hình:

- **Icon cũ (legacy)**: `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png`
  và `ic_launcher_round.png` — dùng cho Android 7.1 trở xuống.
- **Adaptive icon (Android 8+)**: `mipmap-anydpi-v26/ic_launcher.xml` +
  `ic_launcher_foreground.png` (logo được thu nhỏ về đúng "vùng an toàn"
  ~62% khung 108dp để không bị cắt mất viền dù launcher dùng mask hình tròn,
  vuông bo góc hay squircle) + nền trắng `@color/ic_launcher_background`.
- **Icon cho Play Console** (không nằm trong app, dùng khi tạo listing):
  `playstore-assets/play-store-icon-512.png` — 512×512px, nền trắng, không
  trong suốt, đúng chuẩn Google Play yêu cầu.

## Build ra APK / AAB

Mở thư mục này bằng **Android Studio** (khuyên dùng bản mới nhất), để Gradle
tự đồng bộ, sau đó:

- **APK debug** (cài thử trên điện thoại): `Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s)`
- **AAB release** (để tải lên CH Play): `Build ▸ Generate Signed Bundle / APK`
  → chọn **Android App Bundle**, tạo/keystore ký ứng dụng, chọn `release`.

Hoặc dùng dòng lệnh (đã có sẵn Gradle Wrapper):

```bash
# APK debug
./gradlew assembleDebug

# AAB release (cần cấu hình signingConfig trong app/build.gradle
# hoặc dùng Android Studio để ký khi generate)
./gradlew bundleRelease
```

> **Lưu ý về ký ứng dụng**: file build hiện chưa có `signingConfig` cho bản
> release. Khi build AAB để đăng Play Console, hãy dùng chức năng
> **Generate Signed Bundle** của Android Studio để tạo keystore mới (hoặc
> dùng lại keystore cũ nếu đây là bản cập nhật của app đã phát hành), Android
> Studio sẽ tự thêm cấu hình ký vào Gradle.

## Cấu trúc project

```
quangcaoktd-app/
├── app/
│   ├── build.gradle                     # applicationId, dependencies
│   └── src/main/
│       ├── AndroidManifest.xml          # chỉ khai báo quyền INTERNET
│       ├── java/com/quangcaoktd/app/
│       │   ├── MainActivity.java        # WebView + điều hướng + xử lý lỗi
│       │   └── WavyHeaderView.java      # header sóng màu #0277A4
│       └── res/
│           ├── layout/activity_main.xml # header + webview + progress + error view
│           ├── mipmap-*/                # icon các mật độ
│           ├── mipmap-anydpi-v26/       # adaptive icon
│           ├── values/                  # strings, colors, styles
│           ├── drawable/bg_retry_button.xml
│           └── xml/network_security_config.xml
├── build.gradle / settings.gradle / gradle.properties
└── gradle/wrapper/                      # Gradle 8.13
```

## Trước khi đăng CH Play

1. Đổi `versionCode` / `versionName` trong `app/build.gradle` mỗi lần cập nhật.
2. Chuẩn bị ảnh chụp màn hình app, mô tả ngắn/dài, banner theo yêu cầu Play
   Console (icon 512×512 đã có sẵn ở `playstore-assets/`).
3. Điền chính sách quyền riêng tư (Privacy Policy URL) — vì app chỉ dùng
   WebView và quyền `INTERNET`, không thu thập dữ liệu thiết bị, nên chính
   sách bảo mật có thể tham chiếu chính sách hiện có trên quangcaoktd.com.
4. Khai báo mục "Data safety" trên Play Console: app không tự thu thập dữ
   liệu người dùng ngoài những gì website quangcaoktd.com xử lý qua trình
   duyệt (cookie, form liên hệ).
