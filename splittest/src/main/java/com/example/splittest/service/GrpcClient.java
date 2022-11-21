package com.example.splittest.service;

import authserver.common.CheckRequestDTO;
import authserver.common.CheckTestDto;
import com.example.splittest.config.AppProperties;
import io.envoyproxy.envoy.service.auth.v3.AttributeContext;
import io.envoyproxy.envoy.service.auth.v3.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcClient {

  private final AppProperties appProperties;

  public GrpcClient(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  public CheckResponse sendRequest(CheckTestDto dto) {
    CheckRequestDTO requestDTO = dto.getRequest();

    AttributeContext.HttpRequest httpRequest =
        AttributeContext.HttpRequest.newBuilder()
            .setMethod(requestDTO.getHttpMethod())
            .setPath(requestDTO.getRequestPath())
            .putAllHeaders(requestDTO.getHeadersMap())
            .build();

    AttributeContext.Request request =
        AttributeContext.Request.newBuilder().setHttp(httpRequest).build();

    AttributeContext attributeContext = AttributeContext.newBuilder().setRequest(request).build();

    CheckRequest checkRequest = CheckRequest.newBuilder().setAttributes(attributeContext).build();

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress(
                appProperties.getAuthzHostname(), appProperties.getAuthzGrpcPort())
            .usePlaintext()
            .build();

    AuthorizationGrpc.AuthorizationBlockingStub stub = AuthorizationGrpc.newBlockingStub(channel);

    CheckResponse response = stub.check(checkRequest);

    channel.shutdown();

    return response;
  }
}
