package com.example.splittest.service;

import com.example.splittest.config.AppProperties;
import com.example.splittest.entity.Mismatch;
import com.example.splittest.repo.MismatchRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@EnableScheduling
@Service
public class ReTestService {

  private final AuthzTestService authzTestService;
  private final MismatchRepository mismatchRepository;
  private final AppProperties appProperties;

  public ReTestService(
      AuthzTestService authzTestService,
      MismatchRepository mismatchRepository,
      AppProperties appProperties) {
    this.authzTestService = authzTestService;
    this.mismatchRepository = mismatchRepository;
    this.appProperties = appProperties;
  }

  @Scheduled(fixedDelay = 5 * 60 * 1000L)
  public void retest() {
    if (!appProperties.isCrsSubscribeEnabled()) {
      return;
    }
    List<Mismatch> mismatches =
        mismatchRepository.findAllByAttemptsBeforeAndResultMismatch(5, true);
    for (Mismatch m : mismatches) {
      log.info("retest: {}", m.getId());
      authzTestService.authzReTestGrpc(m);
    }

    List<Mismatch> mismatches2 = mismatchRepository.findAllByAttemptsBeforeAndTagsMismatch(5, true);
    for (Mismatch m : mismatches2) {
      log.info("retest: {}", m.getId());
      authzTestService.authzReTestGrpc(m);
    }
  }
}
