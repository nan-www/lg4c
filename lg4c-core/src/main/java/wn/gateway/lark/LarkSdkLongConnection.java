package wn.gateway.lark;

import java.io.IOException;

public interface LarkSdkLongConnection {
    void start();

    boolean isConnected();

    void close() throws IOException;
}
