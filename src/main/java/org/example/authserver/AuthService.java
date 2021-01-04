package org.example.authserver;

import com.google.rpc.Status;
import io.envoyproxy.envoy.service.auth.v3.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.envoyproxy.envoy.service.auth.v3.OkHttpResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class AuthService extends AuthorizationGrpc.AuthorizationImplBase {

    private static final Integer OK = 0;
    private static final Integer PERMISSION_DENIED=7;

    @Override
    public void check(CheckRequest request, StreamObserver<CheckResponse> responseObserver) {

        log.info("request: {}", request);

        boolean allow = false;

        String token;
        try {
            Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
            token = headers.get("authorization");
            if ("bearer foo".equalsIgnoreCase(token)){
                allow = true;
            }
        } catch (NullPointerException e){
            log.warn("Can't parse headers");
        }

        CheckResponse response = CheckResponse.newBuilder()
                .setStatus(Status.newBuilder().setCode(getCode(allow)).build())
                .setOkResponse(OkHttpResponse.newBuilder().build())
                .build();

        log.info("response: {}", response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private int getCode(boolean allow) {
        return allow ? OK : PERMISSION_DENIED;
    }

}
