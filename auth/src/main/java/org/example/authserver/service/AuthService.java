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
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.zanzibar.AclFilterService;

@Slf4j
public class AuthService extends AuthorizationGrpc.AuthorizationImplBase {

    private static final Integer OK = 0;
    private static final Integer PERMISSION_DENIED=7;

    private final AclFilterService aclFilterService;

    public AuthService(AclFilterService aclFilterService) {
        this.aclFilterService = aclFilterService;
    }

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {
        log.info("request: {}", request);

        CheckResult result = aclFilterService.checkRequest(request);

        HeaderValueOption allowedTagsHeaders = HeaderValueOption.newBuilder()
                .setHeader(HeaderValue.newBuilder()
                        .setKey("X-ALLOWED-TAGS")
                        .setValue(String.join(",", result.getTags()))
                .build())
                .build();

        CheckResponse response = CheckResponse.newBuilder()
                .setStatus(Status.newBuilder().setCode(getCode(result.isResult())).build())
                .setOkResponse(OkHttpResponse.newBuilder().addHeaders(allowedTagsHeaders).build())
                .build();

        log.info("response: {}", response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private int getCode(boolean allow) {
        return allow ? OK : PERMISSION_DENIED;
    }

}
