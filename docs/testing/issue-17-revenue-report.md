# Issue #17 — Báo cáo doanh thu theo HuongDan.md

Triển khai theo mục **#17.1–#17.4** của `HuongDan.md`, kiểm thử ngày 20/10/2026 (Asia/Saigon).

## Đối chiếu hướng dẫn

- Nhánh `feature/17-revenue-report`, tạo từ `develop`.
- Đúng 3 commit theo thứ tự trong mục #17.2; mỗi commit có `Refs #17` và compile thành công.
- `MovieRevenueDTO`, `ReportResult`, `ReportService`, `ReportServiceImpl`, `AdminReportController`, `admin/report.html` dùng đúng các khối mã trong hướng dẫn.
- Q7 dùng `Ticket` → `Booking`/`Showtime` → `Movie`, constructor expression, nhóm theo ID/tên phim, doanh thu giảm dần.
- Q8 fetch customer/showtime/movie, đơn CONFIRMED giảm dần theo ngày đặt.
- Thay dòng comment menu Báo cáo bằng link `/admin/reports`.
- Bổ sung kiểm thử và ảnh evidence; không mở rộng chức năng dashboard của issue #20.

## Kết quả kiểm thử

Java 21, Spring Boot 4.1.1, H2 ở chế độ MSSQLServer.

`mvnw.cmd -B -ntp clean verify`: **35 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**, gồm 11 trường hợp cho report.

| Nhóm | Đã xác minh |
|---|---|
| Service/query | Chỉ CONFIRMED; lọc theo ngày đặt thay vì ngày chiếu; tính đầu ngày và cuối ngày, loại đầu ngày kế tiếp; cùng ngày; nhiều suất của một phim; tách phim trùng tên; dùng giá vé lịch sử; tổng đơn/vé/doanh thu chính xác |
| Validation | Null ngày và StartDate > EndDate bị từ chối ở service trước khi query |
| MVC | Mặc định đầu tháng đến hôm nay; ngày bỏ trống dùng mặc định riêng theo controller mẫu; đảo ngày hiển thị lỗi; ngày sai định dạng theo xử lý lỗi sẵn có; kỳ rỗng hiện thông báo và ẩn bảng; render tổng/link chi tiết; chưa đăng nhập về login, CUSTOMER nhận 403 |
| Chromium | Đăng nhập thật, mở từ menu; 3 đơn / 6 vé / 440,000đ; phim 210,000 > 140,000 > 90,000đ; đơn #3 > #2 > #1; link chi tiết; cùng ngày 1 đơn / 3 vé / 210,000đ; kỳ năm ngoái không có dữ liệu; đảo ngày báo lỗi; thiếu ngày dùng mặc định; render khi tắt JavaScript |

Fixture H2 sử dụng `23:59:59.999999`, đúng độ chính xác timestamp(6). Khoảng truy vấn là `[StartDate 00:00, EndDate + 1 ngày 00:00)`. Định dạng tiền và giao diện giữ theo mẫu hướng dẫn.

## Chạy lại

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\mvnw.cmd -B -ntp clean verify
```

Chạy server riêng với H2 mới, không dùng database local/production:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.useTestClasspath=true' '-Dspring-boot.run.profiles=test' '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' '-Dspring-boot.run.arguments=--server.port=8086 --spring.jpa.show-sql=false'
```

Trong terminal có Node.js, Playwright và Chromium:

```powershell
node src/test/browser/revenue-report.cjs
```

Hỗ trợ các biến `PLAYWRIGHT_MODULE`, `CHROMIUM_PATH`, `BASE_URL`, `EVIDENCE_DIR`. Script dùng tài khoản mẫu DataSeeder trên server kiểm thử mới.

## Evidence

Ảnh chụp trực tiếp từ Chromium desktop 1440×1080, không chỉnh sửa, chỉ có dữ liệu mẫu:

1. [Báo cáo đầy đủ](../screenshots/issue-17/01-revenue-report.png).
2. [Báo cáo cùng ngày](../screenshots/issue-17/02-single-day.png).
3. [Khoảng năm ngoái không có dữ liệu](../screenshots/issue-17/03-empty-period.png).
4. [StartDate > EndDate hiển thị lỗi](../screenshots/issue-17/04-invalid-period.png).

Chưa chạy bộ kiểm thử trên SQL Server thật. Reviewer độc lập thực hiện review trước khi merge theo mục #17.5–#17.6.
