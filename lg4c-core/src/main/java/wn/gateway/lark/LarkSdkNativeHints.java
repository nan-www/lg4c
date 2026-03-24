package wn.gateway.lark;

import com.lark.oapi.event.model.Event;
import com.lark.oapi.event.model.Fuzzy;
import com.lark.oapi.event.model.Header;
import com.lark.oapi.ws.Client;
import com.lark.oapi.ws.model.ClientConfig;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        Client.class,
        ClientConfig.class,
        Fuzzy.class,
        Event.class,
        Header.class
})
final class LarkSdkNativeHints {
    private LarkSdkNativeHints() {
    }
}
