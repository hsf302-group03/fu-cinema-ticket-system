# FU Cinema Ticket System — Version 2.0.0

Hệ thống quản lý & bán vé xem phim tích hợp **Website Thymeleaf** và **Ứng dụng Desktop JavaFX** — Assignment 01 & 02 môn **HSF302**.

---

## 1. Công nghệ sử dụng
- **Backend & Core:** Java 21 · Spring Boot 4.1.1 · Spring Data JPA (Hibernate 7) · Bean Validation · Virtual Threads.
- **Cơ sở dữ liệu:** SQL Server (database `FUCinemaTicketDB`, chung 1 database cho cả Web và Desktop).
- **Giao diện Web:** Spring MVC · Thymeleaf · Bootstrap 5 (CDN).
- **Giao diện Desktop (Quầy vé):** JavaFX 21.0.5 (FXML + CSS) · Spring Boot Integration (`WebApplicationType.NONE`, profile `desktop`).
- **Bảo mật & Phân quyền:** BCryptPasswordEncoder, phân quyền theo Role (ADMIN và CUSTOMER).

---

## 2. Hướng dẫn cài đặt & Khởi chạy

### 2.1. Yêu cầu môi trường
- **JDK:** Java 21 (LTS).
- **Maven:** 3.9+.
- **Database:** Microsoft SQL Server 2019+ (bật TCP/IP cổng 1433).

### 2.2. Cấu hình cơ sở dữ liệu
1. Tạo database trong SSMS:
   ```sql
   CREATE DATABASE FUCinemaTicketDB;
   ```
2. Tạo file cấu hình cá nhân `src/main/resources/application-local.properties` (đã nằm trong `.gitignore`):
   ```properties
   spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=FUCinemaTicketDB;encrypt=true;trustServerCertificate=true
   spring.datasource.username=sa
   spring.datasource.password=sa
   ```

### 2.3. Khởi chạy đồng thời Web và Desktop (TECH-18)
Hệ thống cho phép chạy **đồng thời** cả Web và Desktop trên cùng database mà không xung đột:

* **Chạy ứng dụng Web (cổng 8080):**
  - Trong IntelliJ: Run `com.fu.cinema.FuCinemaTicketSystemApplication`
  - Hoặc qua dòng lệnh:
    ```bash
    mvn spring-boot:run
    ```
    Truy cập website tại: `http://localhost:8080`

* **Chạy ứng dụng Desktop (Quầy vé):**
  - Trong IntelliJ: Run `com.fu.cinema.desktop.DesktopLauncher`
  - Hoặc qua dòng lệnh:
    ```bash
    mvn spring-boot:run -Dspring-boot.run.main-class=com.fu.cinema.desktop.DesktopLauncher
    ```

* **Kiểm tra build và kiểm thử tự động (dùng H2 in-memory):**
  ```bash
  mvn clean verify
  ```

---

## 3. Tài khoản kiểm thử
| Vai trò | Tên đăng nhập | Mật khẩu | Quyền truy cập |
|---|---|---|---|
| **ADMIN** | `admin@fucinema.vn` | `Admin@123` | Toàn quyền trên Web `/admin` và **Ứng dụng Desktop** |
| **CUSTOMER** | `an.nv@gmail.com` | `Customer@123` | Chỉ dùng trên Web (đặt vé online, lịch sử đơn) |
| **CUSTOMER** | `binh.tt@gmail.com` | `Customer@123` | 15 tuổi — dùng test quy tắc độ tuổi BR-11 (phim T16/T18) |

> Ứng dụng Desktop chỉ cho phép tài khoản **ADMIN** đăng nhập (quy tắc **BR-D1**). Tài khoản CUSTOMER khi đăng nhập Desktop sẽ bị từ chối.

---

## 4. Phân công thành viên (Assignment 02)

| Thành viên | Vai trò | Module & TODO phụ trách |
|---|---|---|
| **A — Đặng Công Hoàng Nhật** | Leader | TODO 33 (JavaFX bootstrap), TODO 34 (Điều hướng & Khung chính), TODO 36 (Đăng nhập & Session), TODO 46 (README, Test & Release) |
| **B — Trương Quang Huy** | Developer | TODO 35 (FxAsync, AlertUtil, FxValidator), TODO 43 (Bán vé tại quầy), TODO 44 (Quản lý đơn), TODO 45 (Báo cáo doanh thu & CSV) |
| **C — Nguyễn Hữu Minh** | Developer | TODO 38 (Quản lý thể loại), TODO 39 (Danh sách & lọc phim), TODO 40 (Dialog thêm/sửa phim) |
| **D — Thành viên D** | Developer | TODO 37 (Dashboard KPI), TODO 41 (Phòng & Suất chiếu), TODO 42 (Component SeatMapView) |

---

## 5. Kết quả kiểm thử kịch bản Desktop (TC-D01 → TC-D20)

| Mã | Kịch bản kiểm thử | Kết quả mong đợi | Trạng thái |
|---|---|---|:---:|
| **TC-D01** | Chạy `DesktopLauncher` | Cửa sổ Desktop mở; log không có Tomcat; Web chạy song song | ✅ PASS |
| **TC-D02** | Đăng nhập `admin@fucinema.vn` sai mật khẩu | Báo lỗi sai thông tin đăng nhập | ✅ PASS |
| **TC-D03** | Đăng nhập bằng tài khoản CUSTOMER | Bị từ chối: Desktop chỉ dành cho nhân viên (BR-D1) | ✅ PASS |
| **TC-D04** | Đăng nhập ADMIN thành công | Vào khung chính; status bar hiện tên; dashboard khớp Web | ✅ PASS |
| **TC-D05** | Tạo suất mới trên Web → Bấm Làm mới trên Desktop | Biểu đồ cập nhật số liệu mới tức thì | ✅ PASS |
| **TC-D06** | Thêm mới → sửa → xoá thể loại | Thao tác thành công, TableView cập nhật | ✅ PASS |
| **TC-D07** | Xoá thể loại còn phim | Báo lỗi BR-05, chặn không cho xoá | ✅ PASS |
| **TC-D08** | Tìm phim (debounce) + lọc thể loại + phân trang | Kết hợp lọc chính xác, đổi bộ lọc về trang 1 | ✅ PASS |
| **TC-D09** | Thêm phim với thời lượng ngoài [30, 300] phút | Báo lỗi Bean Validation cạnh ô, dialog không đóng | ✅ PASS |
| **TC-D10** | Xoá phim đã có suất chiếu | Thông báo "Đã chuyển STOPPED", dòng phim tô xám (BR-06) | ✅ PASS |
| **TC-D11** | Tạo suất chiếu trùng giờ cùng phòng | Báo lỗi chỉ rõ suất chiếu bị trùng giờ (BR-08) | ✅ PASS |
| **TC-D12** | Chọn phim 120 phút, bắt đầu 19:00 | Gợi ý giờ kết thúc tự tính toán là 21:15 | ✅ PASS |
| **TC-D13** | Bán vé tại quầy: chọn khách, suất, 3 ghế, tick "Đã thanh toán" | Tổng tiền = 3 × giá; đơn lưu CONFIRMED; ghế chuyển đã bán | ✅ PASS |
| **TC-D14** | Chọn ghế thứ 9 trên sơ đồ ghế | Tự động bỏ chọn, cảnh báo tối đa 8 ghế (BR-10) | ✅ PASS |
| **TC-D15** | Khách 15 tuổi mua phim T18 | Báo lỗi không đủ tuổi xem phim (BR-11) | ✅ PASS |
| **TC-D16** | Tranh chấp ghế Web ↔ Desktop (cùng đặt 1 ghế) | Chỉ 1 bên thành công, bên còn lại báo ghế đã bán | ✅ PASS |
| **TC-D17** | Lọc đơn PENDING → Xác nhận; đơn CONFIRMED → Huỷ | Trạng thái đổi chuẩn xác theo quy tắc BR-13 | ✅ PASS |
| **TC-D18** | Chọn đơn CANCELLED | Các nút Xác nhận và Huỷ bị disable | ✅ PASS |
| **TC-D19** | Báo cáo doanh thu → Xuất file CSV → Mở bằng Excel | Chart, bảng, tổng khớp web; file CSV hiển thị chuẩn tiếng Việt | ✅ PASS |
| **TC-D20** | Đăng xuất → Đóng ứng dụng | Xoá session, quay về Login; đóng app tiến trình Java kết thúc | ✅ PASS |

---

## 6. Ảnh chụp màn hình ứng dụng Desktop

| Màn hình | Mô tả |
|---|---|
| **Đăng nhập Desktop** | Giao diện đăng nhập chuẩn dành riêng cho nhân viên ADMIN (TODO 36). |
| **Dashboard** | Thẻ KPI tổng quan và biểu đồ suất chiếu hôm nay theo từng phòng (TODO 37). |
| **Sơ đồ ghế (SeatMapView)** | Component sơ đồ ghế trực quan, ghế đã bán khóa disable, giới hạn 8 ghế (TODO 42). |
| **Bán vé tại quầy** | Chuỗi liên hoàn chọn khách hàng, phim, suất chiếu, sơ đồ ghế và thanh toán (TODO 43). |
| **Quản lý đơn đặt vé** | Bảng đơn vé phân trang, lọc đa tiêu chí, tô màu trạng thái, xác nhận/hủy đơn (TODO 44). |
| **Báo cáo doanh thu & Xuất CSV** | Biểu đồ cột BarChart, biểu đồ tròn PieChart, bảng đơn CONFIRMED và xuất CSV (TODO 45). |

---

## 7. Quy trình Git & Release
- Quy trình chuẩn Git Flow: `main` ← `release/v2.0.0` ← `develop` ← `feature/*`.
- Tuân thủ quy chuẩn Conventional Commits với scope `desktop`:
  - `feat(desktop): add seat map component Refs #52`
  - `feat(desktop): add counter sale screen Refs #53`
  - `feat(desktop): add booking management screen Refs #54`
  - `feat(desktop): add revenue report and csv export Refs #55`
  - `docs(desktop): update README, tests and prepare release v2.0.0 Refs #56`
