package org.blackum.blackaddons.util;

public class BotResult<T> {
    private final T data;
    private final String error;
    private final boolean loading;

    private BotResult(T data, String error, boolean loading) {
        this.data = data;
        this.error = error;
        this.loading = loading;
    }

    public static <T> BotResult<T> success(T data) {
        return new BotResult<>(data, null, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> BotResult<T> error(String message) {
        return new BotResult<>(null, message, false);
    }

    @SuppressWarnings("unchecked")
    public static <T> BotResult<T> loading() {
        return new BotResult<>(null, null, true);
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean isSuccess() {
        return data != null && error == null && !loading;
    }

    public boolean hasError() {
        return error != null;
    }
}
