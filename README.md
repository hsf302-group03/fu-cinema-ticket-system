# FU Cinema Ticket System

Website quản lý & bán vé xem phim — Assignment 01 môn **HSF302**.

## Công nghệ
- Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate 7) · Thymeleaf · Bean Validation
- SQL Server (database `FUCinemaTicketDB`, bảng tạo bằng `ddl-auto=update`)
- Kiến trúc 3-Layer: `controller` → `service` (interface + impl) → `repository` → `entity`
- Đăng nhập bằng HttpSession, phân quyền bằng `HandlerInterceptor`, mật khẩu BCrypt (`spring-security-crypto`)
- Bootstrap 5 (CDN)

## Cài đặt & chạy
Yêu cầu: JDK 21+, Maven 3.9+, SQL Server 2019+ (bật TCP/IP cổng 1433, có login SQL).

```bash
git clone https://github.com/<org>/fu-cinema-ticket-system.git
cd fu-cinema-ticket-system
```

1. Tạo database trong SSMS:
   ```sql
   CREATE DATABASE FUCinemaTicketDB;
   ```
2. Tạo file cấu hình cá nhân (đã nằm trong `.gitignore`, **không commit**):
   ```bash
   cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
   ```
   Sửa `spring.datasource.username` / `spring.datasource.password` theo SQL Server của máy bạn.
3. Chạy:
   ```bash
   mvn spring-boot:run
   ```
   Mở http://localhost:8080. Lần chạy đầu tiên, `DataSeeder` tự tạo bảng và dữ liệu mẫu.
4. Kiểm tra build + test (dùng H2, không cần SQL Server):
   ```bash
   mvn clean verify
   ```

## Tài khoản mẫu
| Vai trò | Tên đăng nhập | Mật khẩu | Ghi chú |
|---|---|---|---|
| ADMIN | `admin@fucinema.vn` | `Admin@123` | |
| CUSTOMER | `an.nv@gmail.com` | `Customer@123` | Người lớn |
| CUSTOMER | `binh.tt@gmail.com` | `Customer@123` | 15 tuổi — dùng để test BR-11 (phim T16/T18) |

## Chức năng chính
- **Khách:** xem phim đang chiếu / sắp chiếu, tìm & lọc theo thể loại, xem chi tiết + lịch chiếu, đăng ký, đăng nhập.
- **Customer:** đặt vé nhiều ghế trên sơ đồ ghế, lịch sử & huỷ đơn, hồ sơ cá nhân, đổi mật khẩu, đánh giá phim (bonus).
- **Admin:** dashboard, quản lý khách hàng, thể loại, phim, phòng chiếu, suất chiếu (chống trùng giờ), đơn đặt vé, báo cáo doanh thu theo khoảng ngày.

## Thành viên & phân công
| Thành viên               | MSSV     | Vai trò | Module | TODO |
|--------------------------|----------|---|---|---|
| A — Đặng Công Hoàng Nhật | DE190259 | Leader | Khởi tạo project, đăng nhập/đăng ký/phân quyền, README, release | 1–7, 9–10, 13–15, 32 |
| B — Trương Quang Huy     | DE190381 | Developer | Xử lý lỗi, quản lý phim, thể loại, trang chủ và đặt vé | 11–12, 18–21, 24–25, 27–29, 31 |
| C — Nguyễn Hữu Minh      | DE190686 | Developer | Seed dữ liệu, quản lý khách hàng, phòng chiếu, suất chiếu và báo cáo | 8, 16–17, 22–23, 26, 30 |

## Ảnh chụp màn hình
| Màn hình | Ảnh |
|---|---|
| Trang chủ | ![home](docs/screenshots/home.png) |
| Chọn ghế | ![seats.png](docs/screenshots/seats.png)|
| Quản lý suất chiếu | ![showtimes](docs/screenshots/admin-showtimes.png) |
| Báo cáo doanh thu | ![report](docs/screenshots/report.png) |

## Quy trình làm việc nhóm
Git Flow rút gọn (`main` ← `release/*` ← `develop` ← `feature/*`), Conventional Commits, mọi thay đổi qua Pull Request có review (reviewer xoay vòng giữa các thành viên).


