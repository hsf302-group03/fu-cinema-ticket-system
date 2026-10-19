# Issue #14 — Seat selection & booking

Kiểm thử ngày 19/10/2026, Java 21, Spring Boot 4.1.1, H2 2.4.240 ở chế độ MSSQLServer; Chromium desktop 1440×1080 và mobile 390×844.

## Kết quả

`./mvnw clean verify`: **24 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**.

| Nhóm | Bằng chứng |
|---|---|
| Unit validation (18 cases) | Thiếu suất/ghế, null/rỗng, mã ghế sai hoặc ngoài phòng, quá 8 ghế, suất đã bắt đầu/hủy, ghế đã bán, tuổi trước và đúng sinh nhật, chuẩn hóa/bỏ trùng ghế, giá từ server |
| Integration/MVC (5 tests) | Ghế PENDING/CONFIRMED được giữ, CANCELLED được bán lại; 2 transaction đồng thời chỉ 1 thành công; lỗi chèn vé thứ hai rollback cả đơn và vé thứ nhất; phân quyền; validation form; chặn xem đơn của người khác; bỏ qua customerId/tổng tiền giả từ form |
| Application (1 test) | Spring context khởi động và seed dữ liệu thành công |
| Browser | Chọn nhiều ghế và tổng tiền; đặt thành công; trang cũ bị tranh ghế; giới hạn tuổi; giới hạn 8 ghế; mobile không tràn trang; đặt vé khi tắt JavaScript |

Test rollback cố ý thêm CHECK constraint tạm thời để từ chối vé H12 sau khi H11 đã chèn; sau exception, số Booking và Ticket không tăng. Constraint được gỡ trong `finally`.

Test concurrency chạy hai lời gọi service trong hai thread, cùng bắt đầu bằng latch, sử dụng transaction và database lock thật. Kết quả: một đơn thành công, một lỗi “Ghế B1 đã bán”, chỉ một vé được lưu.

## Chạy lại

```powershell
.\mvnw.cmd clean verify
```

Chạy server riêng với DB H2 mới; profile `test` dùng H2 chỉ có trong test classpath:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.useTestClasspath=true' '-Dspring-boot.run.profiles=test' '-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false' '-Dspring-boot.run.arguments=--server.port=8085 --spring.jpa.show-sql=false'
```

Trong terminal khác, dùng Node.js có package `playwright` và Chromium đã cài:

```powershell
node src/test/browser/seat-booking.cjs
```

Nếu Playwright nằm ngoài `node_modules` của dự án, đặt `PLAYWRIGHT_MODULE` thành đường dẫn module. Có thể chỉ định `CHROMIUM_PATH`, `BASE_URL` và `EVIDENCE_DIR`. Script dùng tài khoản mẫu của DataSeeder qua endpoint đăng nhập, chạy trên DB kiểm thử mới mỗi lần và tự lưu ảnh. Không chạy script trên DB production.

## Ảnh chụp trực tiếp từ trình duyệt

1. [Sơ đồ ghế: E5/E6 đã bán, F1 được trả lại từ đơn hủy](../screenshots/issue-14/01-seat-map.png).
2. [Chọn F1/F2: tổng 160.000 đ](../screenshots/issue-14/02-multiple-seats-total.png).
3. [Đơn đặt vé thành công và trạng thái PENDING](../screenshots/issue-14/03-booking-success.png).
4. [Khách thứ hai gửi sơ đồ cũ: F1 đã bán](../screenshots/issue-14/04-stale-seat-conflict.png).
5. [Khách 15 tuổi bị từ chối phim T18](../screenshots/issue-14/05-age-validation.png).
6. [Giao diện mobile 390px; sơ đồ cuộn ngang trong vùng riêng](../screenshots/issue-14/06-mobile-seat-map.png).

Ảnh không qua chỉnh sửa; chỉ chứa tài khoản và dữ liệu mẫu. Kiểm thử database tự động và trình duyệt trong lần này dùng H2. Chưa chạy lại trên SQL Server thật.
