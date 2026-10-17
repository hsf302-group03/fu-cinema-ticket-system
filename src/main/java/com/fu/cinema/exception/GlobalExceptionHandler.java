package com.fu.cinema.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class,
            MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex) {
        log.debug("Not found: {}", ex.getMessage());
        return "error/404";
    }

    /** Lỗi nghiệp vụ không được controller bắt → quay lại trang trước kèm flash message. */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        String appRoot = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
        String current = request.getRequestURL().toString();
        // Chỉ quay lại trang trong cùng website và KHÁC trang đang lỗi (tránh redirect vòng lặp).
        // Dấu "/" ở cuối appRoot là bắt buộc: chặn referer giả mạo kiểu "http://host:port.evil.com"
        boolean usable = referer != null
                && referer.startsWith(appRoot + "/")
                && !referer.split("\\?")[0].equals(current);
        return "redirect:" + (usable ? referer : "/");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleOther(Exception ex) {
        log.error("Unexpected error", ex);                                   // log chi tiết, KHÔNG hiển thị stack trace
        return "error/500";
    }
}
