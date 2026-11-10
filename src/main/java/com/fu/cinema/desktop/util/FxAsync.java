package com.fu.cinema.desktop.util;

import javafx.concurrent.Task;
import javafx.scene.Node;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Tiện ích thực thi tác vụ bất đồng bộ trên Virtual Threads (TECH-15, TODO 35).
 * Mọi lời gọi Facade chạy trong Task, callback cập nhật UI trên FX Application Thread.
 */
@Component
public class FxAsync implements DisposableBean {

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
                return work.call(); // Background thread (virtual thread)
            }
        };
        if (busy != null) {
            busy.setDisable(true);
        }
        task.setOnSucceeded(e -> { // FX Application Thread
            if (busy != null) busy.setDisable(false);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> { // FX Application Thread
            if (busy != null) busy.setDisable(false);
            onError.accept(task.getException());
        });
        executor.submit(task);
        return task;
    }

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

