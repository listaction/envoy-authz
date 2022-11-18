package org.example.authserver.service;

import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.envoyproxy.envoy.service.auth.v3.DeniedHttpResponse;
import io.envoyproxy.envoy.service.auth.v3.OkHttpResponse;
import io.envoyproxy.envoy.type.v3.HttpStatus;
import io.envoyproxy.envoy.type.v3.StatusCode;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.example.authserver.config.Constants;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.zanzibar.AclFilterService;
import org.example.authserver.service.zanzibar.TokenService;
import org.springframework.boot.configurationprocessor.json.JSONException;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class AuthService extends AuthorizationGrpc.AuthorizationImplBase {

  private static final Integer OK = 0;
  private static final Integer PERMISSION_DENIED = 7;
  private static final Integer UNAUTHORIZED = 16;
  private static final String TRACE_ID = "TRACEPARENT";
  private static final String SPAN_ID = "X-KEYCLOAK-REALM";

  private final AclFilterService aclFilterService;
  private final RedisService redisService;
  private final TokenService tokenService;

  @Override
  public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
    log.info("request: {} {}",
        request.getAttributes().getRequest().getHttp().getMethod(),
        request.getAttributes().getRequest().getHttp().getPath());

    CheckResponse unauthorizedCheckResult = validateTokenWithSignOutRequest(request);
    if (unauthorizedCheckResult != null) {
      responseObserver.onNext(unauthorizedCheckResult);
      responseObserver.onCompleted();

      return;
    }

    CheckResult result = aclFilterService.checkRequest(request);

    final Pair<String, String> traceAndSpan = getTraceAndSpan(request);
    result.setTraceId(traceAndSpan.getLeft());
    result.setSpanId(traceAndSpan.getRight());

    HeaderValue headerAllowedTags =
        HeaderValue.newBuilder()
            .setKey("X-ALLOWED-TAGS")
            .setValue(String.join(",", result.getTags()))
            .build();

    HeaderValueOption headers = HeaderValueOption.newBuilder().setHeader(headerAllowedTags).build();

    CheckResponse response =
        CheckResponse.newBuilder()
            .setStatus(Status.newBuilder().setCode(getCode(result.isResult())).build())
            .setOkResponse(OkHttpResponse.newBuilder().addHeaders(headers).build())
            .build();

    log.info("response: {}", response);

    if (result.isMappingsPresent()) {
      log.info("request allowed: {}", result.isResult());

      if (!result.isResult()) {
        log.trace("REJECTED by mapping id: {}", result.getRejectedWithMappingId());
      }

    } else {
      log.warn(
          "NO MAPPINGS found for {} {}",
          request.getAttributes().getRequest().getHttp().getMethod(),
          request.getAttributes().getRequest().getHttp().getPath());
    }

    try {
      log.info(result.eventsToJson());
    } catch (JSONException e) {
      e.printStackTrace();
    }

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private int getCode(boolean allow) {
    return allow ? OK : PERMISSION_DENIED;
  }

  private CheckResponse validateTokenWithSignOutRequest(CheckRequest request) {
    try {
      Claims claims = tokenService.getAllClaimsFromRequest(request);
      String key = null;
      if (claims != null && claims.get("jti") != null) {
        key = String.format(Constants.SIGNOUT_REDIS_KEY, claims.get("jti").toString());
      }

      if (key == null) {
        return null;
      }

      if (redisService.exists(key)) {
        return CheckResponse.newBuilder()
            .setStatus(Status.newBuilder().setCode(UNAUTHORIZED))
            .setDeniedResponse(
                DeniedHttpResponse.newBuilder()
                    .setStatus(HttpStatus.newBuilder().setCode(StatusCode.Unauthorized).build())
                    .build())
            .build();
      }

    } catch (Exception ex) {
      log.warn("Redis service is unavailable");
    }

    return null;
  }
  private Pair<String, String> getTraceAndSpan(CheckRequest checkRequest) {
    Map<String, String> headersMap = checkRequest.getAttributes().getRequest().getHttp().getHeadersMap();
    String spanId = headersMap.get(SPAN_ID);
    String traceId = headersMap.get(TRACE_ID);
    return Pair.of(traceId, spanId);
  }
}
