package com.fu.cinema.desktop.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import javafx.css.PseudoClass;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FxValidator {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private final Validator validator; // Bean Validator của Spring Boot

    /** @return map tên field -> thông báo lỗi (rỗng nếu form hợp lệ) */
    public Map<String, String> validate(Object form) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<Object> v : validator.validate(form)) {
            errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
        }
        return errors;
    }

    /** Tô đỏ viền + hiển thị tooltip cho ô lỗi, xoá lỗi ở các ô hợp lệ */
    public boolean mark(Map<String, String> errors, Map<String, Control> fields) {
        fields.forEach((name, control) -> {
            String msg = errors.get(name);
            control.pseudoClassStateChanged(ERROR, msg != null);
            control.setTooltip(msg == null ? null : new Tooltip(msg));
        });
        return errors.isEmpty();
    }
}