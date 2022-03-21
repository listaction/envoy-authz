package org.example.authserver.service;

import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.*;
import io.envoyproxy.envoy.type.v3.HttpStatus;
import io.envoyproxy.envoy.type.v3.StatusCode;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.config.Constants;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.zanzibar.AclFilterService;
import org.example.authserver.service.zanzibar.TokenService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AuthService extends AuthorizationGrpc.AuthorizationImplBase {

    private static final Integer OK = 0;
    private static final Integer PERMISSION_DENIED = 7;
    private static final Integer UNAUTHORIZED = 16;

    private final AclFilterService aclFilterService;
    private final RedisService redisService;
    private final TokenService tokenService;

    public AuthService(AclFilterService aclFilterService, RedisService redisService, TokenService tokenService) {
        this.aclFilterService = aclFilterService;
        this.redisService = redisService;
        this.tokenService = tokenService;
    }

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        log.info("request: {} {}",
                request.getAttributes().getRequest().getHttp().getMethod(),
                request.getAttributes().getRequest().getHttp().getPath()
        );

        CheckResponse unauthorizedCheckResult = validateTokenWithSignOutRequest(request);
        if (unauthorizedCheckResult != null) {
            responseObserver.onNext(unauthorizedCheckResult);
            responseObserver.onCompleted();

            return;
        }

        CheckResult result = aclFilterService.checkRequest(request);

        HeaderValue headerAllowedTags = HeaderValue.newBuilder()
                .setKey("X-ALLOWED-TAGS")
                .setValue(String.join(",", result.getTags()))
                .build();

        HeaderValueOption headers = HeaderValueOption.newBuilder()
                .setHeader(headerAllowedTags)
                .build();

        CheckResponse response = CheckResponse.newBuilder()
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
            log.warn("NO MAPPINGS found for {} {}",
                    request.getAttributes().getRequest().getHttp().getMethod(),
                    request.getAttributes().getRequest().getHttp().getPath()
            );
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
                                        .setStatus(HttpStatus.newBuilder()
                                                .setCode(StatusCode.Unauthorized)
                                                .build())
                                        .build())
                        .build();
            }

        } catch (Exception ex) {
            log.warn("Redis service is unavailable");
        }

        return null;
    }

}
