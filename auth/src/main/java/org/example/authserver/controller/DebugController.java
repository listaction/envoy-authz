package org.example.authserver.controller;

import com.google.common.base.Stopwatch;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.entity.CheckResult;
import org.example.authserver.service.RelationsService;
import org.example.authserver.service.model.LocalCache;
import org.example.authserver.service.zanzibar.Zanzibar;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/debug")
public class DebugController {

  private final RelationsService relationsService;
  private final Zanzibar zanzibar;

  public DebugController(RelationsService relationsService, Zanzibar zanzibar) {
    this.relationsService = relationsService;
    this.zanzibar = zanzibar;
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
}
