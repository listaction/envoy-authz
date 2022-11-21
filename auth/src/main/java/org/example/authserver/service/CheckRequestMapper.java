package org.example.authserver.service;

import authserver.common.CheckRequestDTO;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;

public class CheckRequestMapper {

  public static CheckRequestDTO request2dto(CheckRequest request) {
    return CheckRequestDTO.builder()
        .httpMethod(request.getAttributes().getRequest().getHttp().getMethod())
        .requestPath(request.getAttributes().getRequest().getHttp().getPath())
        .headersMap(request.getAttributes().getRequest().getHttp().getHeadersMap())
        .build();
  }
}
