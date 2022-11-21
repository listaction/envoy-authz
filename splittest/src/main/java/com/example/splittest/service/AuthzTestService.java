package com.example.splittest.service;

import authserver.common.CheckTestDto;
import com.example.splittest.config.MeterService;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthzTestService {

  private final String X_ALLOWED_TAGS_HEADER = "X-ALLOWED-TAGS";
  private final GrpcClient grpcClient;
  private final MeterService meterService;

  public AuthzTestService(GrpcClient grpcClient, MeterService meterService) {
    this.grpcClient = grpcClient;
    this.meterService = meterService;
  }

  public void authzTest(CheckTestDto dto) {
    try {
      CheckResponse checkResponse = grpcClient.sendRequest(dto);
      compareResults(dto, checkResponse);
    } catch (Exception e) {
      log.warn("Can't call", e);
    }
  }

  private void compareResults(CheckTestDto dto, CheckResponse checkResponse) {
    if (dto.isResult() != checkResponse.hasOkResponse()) {
      log.info(
          "result mismatch: expected {}, actual: {}, request: {}",
          dto.isResult(),
          checkResponse.hasOkResponse(),
          dto);
      meterService.countAuthzMismatch();
      return;
    }
    if (dto.isResult()) {
      List<HeaderValueOption> actualResponseHeadersList =
          checkResponse.getOkResponse().getHeadersList();
      String actualTags = getActualHeader(actualResponseHeadersList, X_ALLOWED_TAGS_HEADER);
      String expectedTags = dto.getResultHeaders().getOrDefault(X_ALLOWED_TAGS_HEADER, "");
      if (!expectedTags.equals(actualTags)) {
        log.info(
            "tags mismatch: expected {}, actual: {}, request: {}", expectedTags, actualTags, dto);
        meterService.countAuthzMismatch();
        return;
      }
    }

    meterService.countAuthzOk();
    log.info("OK: {}", dto);
  }

  private String getActualHeader(List<HeaderValueOption> list, String headerName) {
    for (HeaderValueOption option : list) {
      if (option.getHeader().getKey().equalsIgnoreCase(headerName)) {
        return option.getHeader().getValue();
      }
    }
    return null;
  }
}
