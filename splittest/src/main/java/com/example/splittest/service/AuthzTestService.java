package com.example.splittest.service;

import authserver.common.CheckRequestDTO;
import authserver.common.CheckTestDto;
import com.example.splittest.config.AuthzClient;
import com.example.splittest.config.MeterService;
import com.example.splittest.entity.CheckResult;
import com.example.splittest.entity.Mismatch;
import com.example.splittest.repo.MismatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import io.envoyproxy.envoy.config.core.v3.HeaderValueOption;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthzTestService {

  private static final ObjectMapper mapper = new ObjectMapper();

  private final String X_ALLOWED_TAGS_HEADER = "X-ALLOWED-TAGS";
  private final GrpcClient grpcClient;
  private final AuthzClient authzClient;
  private final MeterService meterService;
  private final MismatchRepository mismatchRepository;

  public AuthzTestService(
      GrpcClient grpcClient,
      AuthzClient authzClient,
      MeterService meterService,
      MismatchRepository mismatchRepository) {
    this.grpcClient = grpcClient;
    this.authzClient = authzClient;
    this.meterService = meterService;
    this.mismatchRepository = mismatchRepository;
  }

  public void authzTest(CheckTestDto dto) {
    long debugId = System.nanoTime();

    try {
      CheckResponse checkResponse = grpcClient.sendRequest(dto);
      Gson gson = new Gson();
      String grpcResponseJson = gson.toJson(checkResponse);
      Mismatch mismatch = compareResults(dto, checkResponse, debugId);
      if (mismatch != null) {
        mismatch.setGrpcResponse(grpcResponseJson);
        mismatchRepository.save(mismatch);
      }
      log.info("GRPC response: {}", grpcResponseJson);
    } catch (Exception e) {
      log.warn("Can't call", e);
    }
  }

  public void authzReTest(Mismatch m) {
    CheckTestDto dto = m.getCheckTestDto();

    try {
      CheckResult checkResponse = authzClient.checkQuery("", m.getDebug());
      if (checkResponse.isResult() == m.getExpected()) {
        m.setActualUpdated(m.getExpected());
        m.setResultMismatch(false);

        if (compareTags(m.getExpectedTags(), checkResponse.getAllowedTags())) {
          m.setTagsMismatch(false);
        } else {
          m.setTagsMismatch(true);
        }
      } else {
        m.setResultMismatch(true);
      }

      m.setActualTags(checkResponse.getAllowedTags());
      m.setAttempts(m.getAttempts() + 1);
      m.setUpdated(new Date());
      mismatchRepository.save(m);

      if (!m.getTagsMismatch() && !m.getResultMismatch()) {
        log.info("retest case {} resolved", m.getId());
      }
    } catch (Exception e) {
      log.warn("Can't call", e);
    }
  }

  public void authzReTestGrpc(Mismatch m) {
    CheckTestDto dto = m.getCheckTestDto();

    long debugId = System.nanoTime();
    try {
      CheckResponse checkResponse = grpcClient.sendRequest(dto);
      List<HeaderValueOption> actualResponseHeadersList =
          checkResponse.getOkResponse().getHeadersList();
      String actualTags = getActualHeader(actualResponseHeadersList, X_ALLOWED_TAGS_HEADER);
      String expectedTags = dto.getResultHeaders().getOrDefault(X_ALLOWED_TAGS_HEADER, "");

      Mismatch mismatch = compareResults(dto, checkResponse, debugId);
      if (mismatch != null) {
        m.setActualTags(actualTags);
        m.setAttempts(m.getAttempts() + 1);
        m.setUpdated(new Date());
        mismatchRepository.save(m);

        if (!m.getTagsMismatch() && !m.getResultMismatch()) {
          log.info("retest case {} resolved", m.getId());
        }
      } else {
        m.setActualUpdated(m.getExpected());
        m.setResultMismatch(false);
      }

      m.setActualTags(actualTags);
      m.setAttempts(m.getAttempts() + 1);
      m.setUpdated(new Date());
      mismatchRepository.save(m);
    } catch (Exception e) {
      log.warn("Can't call", e);
    }

    if (!m.getTagsMismatch() && !m.getResultMismatch()) {
      log.info("retest case {} resolved", m.getId());
    }
  }

  private Mismatch compareResults(CheckTestDto dto, CheckResponse checkResponse, long debugId) {
    List<HeaderValueOption> actualResponseHeadersList =
        checkResponse.getOkResponse().getHeadersList();
    String actualTags = getActualHeader(actualResponseHeadersList, X_ALLOWED_TAGS_HEADER);
    String expectedTags = dto.getResultHeaders().getOrDefault(X_ALLOWED_TAGS_HEADER, "");

    boolean actualAccess = (0 == checkResponse.getStatus().getCode());
    if (dto.isResult() != actualAccess) {
      log.info(
          "result mismatch: expected {}, actual: {},\nrequest: {}\nresponse headers: {}",
          dto.isResult(),
          checkResponse.hasOkResponse(),
          dto,
          actualTags);
      log.info("debugId: {}", debugId);
      meterService.countAuthzMismatch();
      return Mismatch.builder()
          .id(debugId)
          .expected(dto.isResult())
          .actual(checkResponse.hasOkResponse())
          .actualUpdated(checkResponse.hasOkResponse())
          .attempts(0)
          .debug(debugRequest(dto.getRequest()))
          .expectedTags(expectedTags)
          .actualTags(actualTags)
          .requestMethod(dto.getRequest().getHttpMethod())
          .requestPath(dto.getRequest().getRequestPath())
          .created(new Date())
          .tenant(dto.getRequest().getTenant())
          .userId(dto.getRequest().getUserId())
          .checkTestDto(dto)
          .tagsMismatch(false)
          .resultMismatch(true)
          .build();
    }
    if (dto.isResult()) {
      if (!compareTags(expectedTags, actualTags)) {
        log.info(
            "tags mismatch: expected {}, actual: {}, request: {}", expectedTags, actualTags, dto);
        log.info("debugId: {}", debugId);
        meterService.countAuthzMismatch();
        return Mismatch.builder()
            .id(debugId)
            .expected(dto.isResult())
            .actual(checkResponse.hasOkResponse())
            .actualUpdated(checkResponse.hasOkResponse())
            .attempts(0)
            .debug(debugRequest(dto.getRequest()))
            .expectedTags(expectedTags)
            .actualTags(actualTags)
            .requestMethod(dto.getRequest().getHttpMethod())
            .requestPath(dto.getRequest().getRequestPath())
            .created(new Date())
            .tenant(dto.getRequest().getTenant())
            .userId(dto.getRequest().getUserId())
            .checkTestDto(dto)
            .tagsMismatch(true)
            .resultMismatch(false)
            .build();
      }
    }

    meterService.countAuthzOk();
    log.trace("OK: {}", dto);
    return null;
  }

  private String debugRequest(CheckRequestDTO request) {
    try {
      Base64.Encoder encoder = Base64.getEncoder();
      byte[] content = mapper.writeValueAsBytes(request);
      return encoder.encodeToString(content);
    } catch (Exception e) {
      return null;
    }
  }

  private boolean compareTags(String expectedTags, String actualTags) {
    if (expectedTags == null && actualTags == null) return true;
    if (expectedTags != null && actualTags == null) return false;
    if (expectedTags == null && actualTags != null) return false;

    List<String> expectedTagsList = Lists.newArrayList(expectedTags.split(","));
    List<String> actualTagsList = Lists.newArrayList(actualTags.split(","));

    expectedTagsList.removeAll(actualTagsList);

    return expectedTagsList.size() == 0;
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
