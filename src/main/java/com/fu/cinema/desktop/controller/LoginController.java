package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class LoginController {

    private final AuthFacade authFacade;
    private final DesktopSession session;
    private final SceneManager sceneManager;
    private final FxAsync fxAsync;

    @FXML private VBox form;
    @FXML private TextField tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Button btnLogin;
    @FXML private ProgressIndicator progress;

    @FXML
    private void initialize() {
        // TECH-17: Nút Đăng nhập tự disable khi 1 trong 2 ô trống
        btnLogin.disableProperty().bind(
                tfUsername.textProperty().isEmpty().or(pfPassword.textProperty().isEmpty())
        );
        Platform.runLater(tfUsername::requestFocus);
    }

    @FXML
    private void onLogin() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();
        progress.setVisible(true);

        // Gọi background thread qua FxAsync, khoá form khi đang tải
        fxAsync.run(() -> authFacade.loginAdmin(username, password),
                user -> {
                    progress.setVisible(false);
                    session.login(user);
                    sceneManager.switchTo(View.MAIN);
                },
                ex -> {
                    progress.setVisible(false);
                    pfPassword.clear();
                    AlertUtil.showError(ex);
                },
                form);
    }
}