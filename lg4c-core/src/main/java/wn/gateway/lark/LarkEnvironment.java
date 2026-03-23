package wn.gateway.lark;

public record LarkEnvironment(
        String baseUrl,
        String websocketUrlOverride,
        String replyUrlOverride) {
    public static final String DEFAULT_BASE_URL = "https://open.feishu.cn";

    public LarkEnvironment {
        baseUrl = normalizeBaseUrl(baseUrl);
        websocketUrlOverride = blankToNull(websocketUrlOverride);
        replyUrlOverride = blankToNull(replyUrlOverride);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_BASE_URL;
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
