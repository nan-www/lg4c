package wn.gateway.lark;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.lark.oapi.core.httpclient.IHttpTransport;
import com.lark.oapi.core.request.FormData;
import com.lark.oapi.core.request.FormDataFile;
import com.lark.oapi.core.request.RawRequest;
import com.lark.oapi.core.response.RawResponse;
import com.lark.oapi.core.utils.Jsons;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.multipart.MultipartForm;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QuarkusVertxHttpTransport implements IHttpTransport {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String BINARY_CONTENT_TYPE = "application/octet-stream";

    private final WebClient webClient;

    @Inject
    public QuarkusVertxHttpTransport(Vertx vertx) {
        this.webClient = WebClient.create(vertx);
    }

    @Override
    public RawResponse execute(RawRequest rawRequest) throws Exception {
        HttpRequest<Buffer> request = webClient.requestAbs(httpMethod(rawRequest.getHttpMethod()), rawRequest.getReqUrl());
        applyHeaders(request, rawRequest.getHeaders());
        applyTimeout(request, rawRequest);

        HttpResponse<Buffer> response = send(request, rawRequest);

        RawResponse rawResponse = new RawResponse();
        rawResponse.setStatusCode(response.statusCode());
        rawResponse.setHeaders(toHeaders(response.headers()));
        rawResponse.setContentType(response.getHeader("content-type"));
        rawResponse.setBody(response.bodyAsBuffer() == null ? new byte[0] : response.bodyAsBuffer().getBytes());
        return rawResponse;
    }

    private HttpResponse<Buffer> send(HttpRequest<Buffer> request, RawRequest rawRequest) throws Exception {
        Object body = rawRequest.getBody();
        if (body == null) {
            return request.send().toCompletionStage().toCompletableFuture().get();
        }
        if (body instanceof FormData formData) {
            return request.sendMultipartForm(toMultipartForm(formData))
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get();
        }
        request.putHeader("content-type", JSON_CONTENT_TYPE);
        Buffer jsonBody = Buffer.buffer(Jsons.LONG_TO_STR.toJson(body));
        return request.sendBuffer(jsonBody).toCompletionStage().toCompletableFuture().get();
    }

    private void applyHeaders(HttpRequest<Buffer> request, Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        headers.forEach((name, values) -> {
            if (name == null || values == null || values.isEmpty()) {
                return;
            }
            request.putHeader(name, values);
        });
    }

    private void applyTimeout(HttpRequest<Buffer> request, RawRequest rawRequest) {
        if (rawRequest.getConfig() == null || rawRequest.getConfig().getTimeOutTimeUnit() == null) {
            return;
        }
        long timeoutMillis = TimeUnit.MILLISECONDS.convert(
                rawRequest.getConfig().getRequestTimeOut(),
                rawRequest.getConfig().getTimeOutTimeUnit());
        if (timeoutMillis > 0) {
            request.timeout(timeoutMillis);
            request.connectTimeout(timeoutMillis);
        }
    }

    private HttpMethod httpMethod(String method) {
        return HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
    }

    private MultipartForm toMultipartForm(FormData formData) {
        MultipartForm multipartForm = MultipartForm.create();
        if (formData.getParams() != null) {
            formData.getParams().forEach((name, value) -> multipartForm.attribute(name, Objects.toString(value, "")));
        }
        if (formData.getFiles() != null) {
            for (FormDataFile file : formData.getFiles()) {
                multipartForm.binaryFileUpload(
                        file.getFieldName(),
                        defaultFileName(file),
                        file.getFile().getAbsolutePath(),
                        file.getType() == null || file.getType().isBlank() ? BINARY_CONTENT_TYPE : file.getType());
            }
        }
        return multipartForm;
    }

    private String defaultFileName(FormDataFile file) {
        if (file.getFileName() != null && !file.getFileName().isBlank()) {
            return file.getFileName();
        }
        if (file.getFile() != null) {
            return file.getFile().getName();
        }
        return "unknown";
    }

    private Map<String, List<String>> toHeaders(MultiMap headers) {
        Map<String, List<String>> mapped = new LinkedHashMap<>();
        for (String name : headers.names()) {
            mapped.put(name, headers.getAll(name));
        }
        return mapped;
    }
}
