package org.example.authserver.controller;

import authserver.common.CheckRequestDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.entity.LocalCache;
import org.example.authserver.service.AuthService;
import org.example.authserver.service.RelationsService;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/debug")
public class DebugController {

  private static final ObjectMapper mapper = new ObjectMapper();

  private final RelationsService relationsService;
  private final Zanzibar zanzibar;
  private final AuthService authService;

  public DebugController(
      RelationsService relationsService, Zanzibar zanzibar, AuthService authService) {
    this.relationsService = relationsService;
    this.zanzibar = zanzibar;
    this.authService = authService;
  }

  @GetMapping("/relations")
  public Set<String> getRelations(
      @RequestParam String namespace, @RequestParam String object, @RequestParam String principal) {
    log.info("get relations: {}:{} @ {}", namespace, object, principal);
    Stopwatch stopwatch = Stopwatch.createStarted();
    Set<String> relations =
        relationsService.getRelations(namespace, object, principal, new LocalCache());
    log.info("get relations finished in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    return relations;
  }

  @GetMapping("/test")
  public boolean test(
      @RequestParam String namespace,
      @RequestParam String object,
      @RequestParam String relation,
      @RequestParam String principal,
      HttpServletResponse response) {
    Stopwatch stopwatch = Stopwatch.createStarted();

    CheckResult result = zanzibar.check(namespace, object, relation, principal, new LocalCache());
    response.addHeader("X-ALLOWED-TAGS", String.join(",", result.getTags()));
    log.info(
        "get relations: {}:{} @ {}, {}ms",
        namespace,
        object,
        principal,
        stopwatch.elapsed(TimeUnit.MILLISECONDS));
    return result.isResult();
  }

  @PostMapping("/query")
  public CheckResult query(@RequestBody CheckRequestDTO dto) throws JsonProcessingException {
    Base64.Encoder encoder = Base64.getEncoder();
    String json = mapper.writeValueAsString(dto);
    String jsonEncoded = encoder.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    System.out.println(jsonEncoded);
    return authService.check(dto, null);
  }

  @GetMapping("/query")
  public CheckResult query(@RequestParam String q) throws IOException {
    Base64.Decoder decoder = Base64.getDecoder();
    byte[] payload = decoder.decode(q);
    CheckRequestDTO dto = mapper.readValue(payload, CheckRequestDTO.class);
    return authService.check(dto, null);
  }
}
