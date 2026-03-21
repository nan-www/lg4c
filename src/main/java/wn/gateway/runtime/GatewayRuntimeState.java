package wn.gateway.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GatewayRuntimeState {
    private static final AtomicBoolean LIVE = new AtomicBoolean(true);
    private static final AtomicBoolean READY = new AtomicBoolean(false);

    private GatewayRuntimeState() {
    }

    public static boolean live() {
        return LIVE.get();
    }

    public static boolean ready() {
        return READY.get();
    }

    public static void markReady(boolean value) {
        READY.set(value);
    }

    public static void markLive(boolean value) {
        LIVE.set(value);
    }
}
