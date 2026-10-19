# Issue #17 — Revenue report

Kiểm thử ngày 20/10/2026 (Asia/Saigon), Java 21, Spring Boot 4.1.1, H2 trong chế độ MSSQLServer, Chromium desktop 1440×1080 và mobile 390×844.

## Kết quả

`mvnw.cmd verify`: **37 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS** (gồm 13 trường hợp mới cho báo cáo).

| Nhóm | Đã xác minh |
|---|---|
| Q7, Q8 và service | Chỉ đơn CONFIRMED; lọc theo ngày đặt thay vì ngày chiếu; đầu ngày được tính, cuối ngày được tính, đầu ngày kế tiếp bị loại; chấp nhận cùng ngày; nhiều suất của một phim được gộp; phim trùng tên được tách theo ID; giá vé lịch sử được giữ khi giá suất thay đổi; không nhân tổng tiền đơn theo số vé; sắp xếp và tổng chính xác |
| Validation | Thiếu một/cả hai ngày, ngày đảo ngược, ngày không tồn tại, sai định dạng, ngày vượt miền SQL Server; không chạy query khi service từ chối ngày |
| MVC | Mặc định đầu tháng đến hôm nay; form giữ ngày đã nhập khi lỗi; trạng thái rỗng có tổng bằng 0; render hai bảng và link chi tiết; khách chưa đăng nhập về login; CUSTOMER nhận 403 |
| Browser | Đăng nhập admin thật, mở từ menu; 3 đơn / 6 vé / 440.000 đ; phim 210.000 > 140.000 > 90.000 đ; đơn #3 > #2 > #1; link chi tiết hoạt động; lọc một ngày 1 đơn / 3 vé / 210.000 đ; kỳ rỗng; lỗi đảo ngày, thiếu ngày, sai định dạng; mobile không tràn trang và bảng cuộn ngang được; form lọc hoạt động khi tắt JavaScript |

H2 lưu timestamp với 6 chữ số thập phân; fixture biên dùng `23:59:59.999999`. Khoảng truy vấn là `[StartDate 00:00, EndDate + 1 ngày 00:00)`.

Q7 cộng `Ticket.price`, nhóm theo `movieId` và tên phim. Q8 fetch customer, showtime, movie và tickets, để trang hoạt động khi `spring.jpa.open-in-view=false`. Các tổng dùng cùng tập đơn CONFIRMED trong kỳ; dữ liệu đặt vé hiện tại đảm bảo tổng tiền đơn bằng tổng giá vé.

## Chạy lại

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
.\mvnw.cmd verify
```

Khởi động server riêng với H2 mới, không dùng database local/production:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.useTestClasspath=true' '-Dspring-boot.run.profiles=test' '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' '-Dspring-boot.run.arguments=--server.port=8086 --spring.jpa.show-sql=false'
```

Trong terminal khác có Node.js, Playwright và Chromium:

```powershell
node src/test/browser/revenue-report.cjs
```

Nếu Playwright không nằm trong node_modules của dự án, đặt `PLAYWRIGHT_MODULE` thành đường dẫn module. Script cũng hỗ trợ `CHROMIUM_PATH`, `BASE_URL`, `EVIDENCE_DIR`. Dùng tài khoản mẫu DataSeeder, chỉ đọc dữ liệu qua giao diện sau đăng nhập; cần một server kiểm thử mới để kết quả khớp fixture.

## Evidence

Ảnh chụp trực tiếp từ Chromium, không chỉnh sửa, chỉ chứa dữ liệu mẫu:

1. [Báo cáo đầy đủ: 3 đơn, 6 vé, 440.000 đ](../screenshots/issue-17/01-revenue-report.png).
2. [Lọc cùng ngày: 1 đơn, 3 vé, 210.000 đ](../screenshots/issue-17/02-single-day.png).
3. [Kỳ không có dữ liệu, các tổng bằng 0](../screenshots/issue-17/03-empty-period.png).
4. [StartDate > EndDate: thông báo lỗi trên form](../screenshots/issue-17/04-invalid-period.png).
5. [Mobile 390px, bảng đơn có vùng cuộn ngang](../screenshots/issue-17/05-mobile-report.png).

Đã kiểm thử trên H2, chưa chạy bộ kiểm thử này trên SQL Server thật. Dashboard thống kê thuộc issue #20, ngoài phạm vi issue #17.
