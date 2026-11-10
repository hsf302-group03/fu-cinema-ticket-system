package com.fu.cinema.desktop;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.facade.BookingFacade;
import com.fu.cinema.desktop.facade.CounterSaleFacade;
import com.fu.cinema.desktop.model.BookingRow;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp cho tầng Facade Desktop (TODO 46).
 * Chạy trên cấu hình test (H2 in-memory, DataSeeder tự tạo dữ liệu mẫu).
 */
@SpringBootTest
@ActiveProfiles("test")
class DesktopFacadeTest {

    @Autowired
    private AuthFacade authFacade;

    @Autowired
    private BookingFacade bookingFacade;

    @Autowired
    private CounterSaleFacade counterSaleFacade;

    @Autowired
    private DesktopSession session;

    @Test
    @DisplayName("TC1 (BR-D1): Tài khoản CUSTOMER không được phép đăng nhập Desktop")
    void customerAccount_cannotLoginDesktop() {
        assertThrows(BusinessException.class, () ->
                authFacade.loginAdmin("an.nv@gmail.com", "Customer@123")
        );
    }

    @Test
    @DisplayName("TC2: Tài khoản ADMIN đăng nhập Desktop thành công")
    void adminAccount_canLogin() {
        SessionUser user = authFacade.loginAdmin("admin@fucinema.vn", "Admin@123");
        assertNotNull(user);
        assertTrue(user.isAdmin(), "Người dùng phải có vai trò ADMIN");
    }

    @Test
    @DisplayName("TC3: Đơn đặt vé ánh xạ Record an toàn trong Transaction, không lỗi Lazy")
    void bookingRows_haveCustomerAndMovie() {
        PageResult<BookingRow> page = bookingFacade.search(null, null, null, null, 0);
        assertNotNull(page);
        assertFalse(page.items().isEmpty(), "Phải có danh sách đơn mẫu từ DataSeeder");
        assertTrue(page.items().stream().allMatch(b -> b.customerName() != null && b.movieTitle() != null),
                "Mọi đơn đặt vé phải tải được tên khách và tên phim mà không ném LazyInitializationException");
    }

    @Test
    @DisplayName("TC4: Thao tác ghi yêu cầu phải có quyền ADMIN trong DesktopSession")
    void writeOperation_requiresAdminSession() {
        session.logout();
        assertThrows(BusinessException.class, () ->
                counterSaleFacade.sell(1L, 1L, List.of("A1"), true)
        );
    }

    @Test
    @DisplayName("TC5: Facade bán vé tải được danh sách phim hợp lệ để lập lịch")
    void counterSale_canLoadMovies() {
        List<MovieItem> movies = counterSaleFacade.movies();
        assertNotNull(movies);
        assertFalse(movies.isEmpty(), "Danh sách phim chiếu phải có dữ liệu");
        assertTrue(movies.stream().allMatch(m -> m.title() != null && m.duration() > 0));
    }
}

