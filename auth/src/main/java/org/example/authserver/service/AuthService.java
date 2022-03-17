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
    private final AppProperties appProperties;
    private final RedisService redisService;
    private final TokenService tokenService;

    private final static Pattern pattern = Pattern.compile("(.*)\\/realms\\/(.*)");

    private static final String SIGNOUT_REDIS_KEY = "%s__%s";

    public AuthService(AclFilterService aclFilterService, AppProperties appProperties, RedisService redisService, TokenService tokenService) {
        this.aclFilterService = aclFilterService;
        this.appProperties = appProperties;
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

            String tenant = getTenant(claims);
            if (tenant != null) {
                String key = String.format(SIGNOUT_REDIS_KEY, tenant, claims.get("jti").toString());
                String expirationTimeValue = redisService.get(key);

                if (expirationTimeValue != null) {
                    // expirationTimeValue is in seconds
                    long currentTimeInSeconds = System.currentTimeMillis() / 1000;
                    if (Long.parseLong(expirationTimeValue) >= currentTimeInSeconds) {
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
                }
            }
        } catch (Exception ex) {
            log.warn("Redis service is unavailable");
        }

        return null;
    }

    private String getTenant(Claims claims) {
        Matcher matcher = pattern.matcher(claims.getIssuer());
        if (matcher.matches() && matcher.groupCount() >= 2) {
            return matcher.group(2);
        }

        return null;
    }

}
