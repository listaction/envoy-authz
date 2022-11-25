package org.example.authserver.service;

import authserver.common.CheckRequestDTO;
import authserver.common.CheckTestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.rpc.Status;
import com.newrelic.api.agent.Trace;
import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.*;
import io.envoyproxy.envoy.type.v3.HttpStatus;
import io.envoyproxy.envoy.type.v3.StatusCode;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.Utils;
import org.example.authserver.config.AppProperties;
import org.example.authserver.config.Constants;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.zanzibar.AclFilterService;
import org.example.authserver.service.zanzibar.TokenService;

@Slf4j
public class AuthService extends AuthorizationGrpc.AuthorizationImplBase {

  private static final Integer OK = 0;
  private static final Integer PERMISSION_DENIED = 7;
  private static final Integer UNAUTHORIZED = 16;
  private static final String TRACE_ID = "TRACEPARENT";
  private static final String TENANT_ID = "X-KEYCLOAK-REALM";

  private final AclFilterService aclFilterService;
  private final RedisService redisService;
  private final TokenService tokenService;
  private final SplitTestService splitTestService;
  private final AppProperties appProperties;

  public AuthService(
      AclFilterService aclFilterService,
      RedisService redisService,
      TokenService tokenService,
      SplitTestService splitTestService,
      AppProperties appProperties) {
    this.aclFilterService = aclFilterService;
    this.redisService = redisService;
    this.tokenService = tokenService;
    this.splitTestService = splitTestService;
    this.appProperties = appProperties;
  }

  @Trace(dispatcher = true)
  @Override
  public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
    log.info(
        "request: {} {}",
        request.getAttributes().getRequest().getHttp().getMethod(),
        request.getAttributes().getRequest().getHttp().getPath());

    CheckResponse unauthorizedCheckResult = validateTokenWithSignOutRequest(request);
    if (unauthorizedCheckResult != null) {
      responseObserver.onNext(unauthorizedCheckResult);
      responseObserver.onCompleted();

      return;
    }

    CheckRequestDTO dto = CheckRequestMapper.request2dto(request);

    CheckResult result;
    try {
      result = aclFilterService.checkRequest(dto);
    } catch (Exception e) {
      log.warn(
          "Can't check request: {} {} ",
          request.getAttributes().getRequest().getHttp().getMethod(),
          request.getAttributes().getRequest().getHttp().getPath());
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);

      result =
          CheckResult.builder()
              .httpMethod(dto.getHttpMethod())
              .requestPath(dto.getRequestPath())
              .result(false)
              .events(Map.of("Exception", e.getMessage(), "Trace", pw.toString()))
              .build();
    }

    String allowedTags = String.join(",", result.getTags());
    result.setTraceId(getTraceId(request));
    result.setTenantId(getTenantId(request));
    result.setAllowedTags(allowedTags);

    HeaderValue headerAllowedTags =
        HeaderValue.newBuilder().setKey("X-ALLOWED-TAGS").setValue(allowedTags).build();

    HeaderValueOption headers = HeaderValueOption.newBuilder().setHeader(headerAllowedTags).build();

    CheckResponse response =
        CheckResponse.newBuilder()
            .setStatus(Status.newBuilder().setCode(getCode(result.isResult())).build())
            .setOkResponse(OkHttpResponse.newBuilder().addHeaders(headers).build())
            .build();

    if (result.isMappingsPresent()) {
      result.getEvents().put("request allowed", String.valueOf(result.isResult()));

      if (!result.isResult()) {
        result.getEvents().put("REJECTED by mapping id", result.getRejectedWithMappingId());
        log.trace("REJECTED by mapping id: {}", result.getRejectedWithMappingId());
      }

    } else {
      result
          .getEvents()
          .put(
              "NO MAPPINGS found",
              String.format(
                  "%s %s",
                  request.getAttributes().getRequest().getHttp().getMethod(),
                  request.getAttributes().getRequest().getHttp().getPath()));
    }

    try {
      Map<String, Object> resultMap = result.getResultMap();

      String logEntry = Utils.prettyPrintObject(resultMap);
      log.info(logEntry);
    } catch (JsonProcessingException e) {
      log.warn("Can't read resultMap", e);
    }

    if (appProperties.isCopyModeEnabled()) {
      splitTestService.submitAsync(
          CheckTestDto.builder()
              .request(dto)
              .result(result.isResult())
              .resultHeaders(Map.of("X-ALLOWED-TAGS", allowedTags))
              .build());
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

  private String getTraceId(CheckRequest checkRequest) {
    Map<String, String> headersMap =
        checkRequest.getAttributes().getRequest().getHttp().getHeadersMap();
    return headersMap.get(TRACE_ID);
  }

  private String getTenantId(CheckRequest checkRequest) {
    Map<String, String> headersMap =
        checkRequest.getAttributes().getRequest().getHttp().getHeadersMap();
    return headersMap.get(TENANT_ID);
  }
}
