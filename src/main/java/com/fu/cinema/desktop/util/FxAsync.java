package com.fu.cinema.desktop.util;

import javafx.concurrent.Task;
import javafx.scene.Node;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Component
public class FxAsync implements DisposableBean {

    // TECH-15: Chạy trên Java 21 Virtual Threads
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess) {
        return run(work, onSuccess, AlertUtil::showError, null);
    }

    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess, Node busy) {
        return run(work, onSuccess, AlertUtil::showError, busy);
    }

    public <T> Task<T> run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError, Node busy) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call(); // Background thread (Virtual thread) — CẤM chạm vào UI ở đây
            }
        };
        if (busy != null) {
            busy.setDisable(true); // Khóa nút/form trong khi đang tải để chống bấm nhiều lần
        }
        task.setOnSucceeded(e -> { // Tự động quay về JavaFX Application Thread
            if (busy != null) busy.setDisable(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {    // Tự động quay về JavaFX Application Thread
            if (busy != null) busy.setDisable(false);
            onError.accept(task.getException());
        });
        executor.submit(task);
        return task;
    }

    /** Cho các thao tác không trả kết quả (ví dụ: xoá, xác nhận...) */
    public Task<Void> runVoid(Runnable work, Runnable onSuccess, Node busy) {
        return run(() -> {
            work.run();
            return null;
        }, v -> onSuccess.run(), AlertUtil::showError, busy);
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}