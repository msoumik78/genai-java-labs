package com.javagenai.lab6.observability;

/**
 * Request-scoped tenant for the chat call. The Micrometer observation stops on the
 * same thread as {@code ChatModel.call}, so the handler can read this.
 */
public final class TenantHolder {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantHolder() {
    }

    public static void set(String tenant) {
        CURRENT.set(tenant == null || tenant.isBlank() ? "anonymous" : tenant);
    }

    public static String current() {
        String tenant = CURRENT.get();
        return tenant == null ? "anonymous" : tenant;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
