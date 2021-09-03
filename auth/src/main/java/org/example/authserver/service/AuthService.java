package org.example.authserver.service;

import com.google.rpc.Status;
import io.envoyproxy.envoy.config.core.v3.HeaderValue;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.envoyproxy.envoy.service.auth.v3.OkHttpResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.zanzibar.AclFilterService;

@Slf4j
public class AuthService extends AuthorizationGrpc.AuthorizationImplBase {

    private static final Integer OK = 0;
    private static final Integer PERMISSION_DENIED=7;

    private final AclFilterService aclFilterService;
    private final AppProperties appProperties;

    public AuthService(AclFilterService aclFilterService, AppProperties appProperties) {
        this.aclFilterService = aclFilterService;
        this.appProperties = appProperties;
    }

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        log.info("request: {} {}",
                request.getAttributes().getRequest().getHttp().getMethod(),
                request.getAttributes().getRequest().getHttp().getPath()

        );
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

            if (!result.isResult()){
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

}
