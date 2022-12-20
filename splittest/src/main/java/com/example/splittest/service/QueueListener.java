package com.example.splittest.service;

import authserver.common.AclOperation;
import authserver.common.AclOperationDto;
import authserver.common.CheckTestDto;
import com.example.splittest.config.AppProperties;
import com.example.splittest.entity.DCheckTestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
@Component
public class QueueListener {

  private final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final DelayQueue<DCheckTestDto> dq = new DelayQueue<>();

  private final String CHECK_REQUEST_QUEUE_NAME = "CheckQueue";
  private final String ACL_QUEUE_NAME = "AclQueue";

  private final Long DELAY_TIME = 3 * 60 * 1000L;

  private final int ACL_PROCESS_THREADS = 5;
  private final ScheduledExecutorService executor =
      Executors.newScheduledThreadPool(ACL_PROCESS_THREADS);
  private final ScheduledExecutorService dqExecutor = Executors.newScheduledThreadPool(1);
  private final AuthzTestService authzTestService;
  private final AuthzAclService authzAclService;
  private final JedisPool jedisPool;
  private final AppProperties appProperties;

  public QueueListener(
      AuthzTestService authzTestService,
      AuthzAclService authzAclService,
      JedisPool jedisPool,
      AppProperties appProperties) {
    this.authzTestService = authzTestService;
    this.authzAclService = authzAclService;
    this.jedisPool = jedisPool;
    this.appProperties = appProperties;

    subscribeChanges();
  }

  private void subscribeChanges() {
    if (appProperties.isAclsSubscribeEnabled()) {
      for (int i = 0; i < ACL_PROCESS_THREADS; i++) {
        executor.scheduleWithFixedDelay(this::processAcls, 0, 5, TimeUnit.SECONDS);
      }
    }

    if (appProperties.isCrsSubscribeEnabled()) {
      executor.execute(this::processChanges);
      dqExecutor.scheduleWithFixedDelay(this::processDelayQueue, 30, 30, TimeUnit.SECONDS);
    }
  }

  private void processDelayQueue() {
    log.info("process CR, queue size: {}", dq.size());
    DCheckTestDto delayObj;
    do {
      delayObj = dq.poll();

      if (delayObj != null) {
        CheckTestDto dto = delayObj.getDto();
        authzTestService.authzTest(dto);
      }
    } while (delayObj != null);
  }

  private void processAcls() {
    boolean r = false;
    do {
      try (Jedis conn = jedisPool.getResource()) {
        r = getAclModifications(conn);
      } catch (Exception e) {
        log.warn("Can't process acl", e);
      }
    } while (r);
  }

  private void processChanges() {
    while (!Thread.interrupted()) {
      try (Jedis conn = jedisPool.getResource()) {
        getCheckRequestAndDelay(conn);
      } catch (Exception e) {
        log.warn("Can't process check request", e);
      }
    }
  }

  private boolean getAclModifications(Jedis conn) throws JsonProcessingException {
    List<String> data = conn.blpop(0, ACL_QUEUE_NAME);
    if (data != null) {
      String payload = data.get(1);
      AclOperationDto dto = mapper.readValue(payload, AclOperationDto.class);
      for (int i = 0; i < 5; i++) {
        try {
          if (AclOperation.DEL.equals(dto.getOp())) {
            authzAclService.deleteAcl(dto.getAcl());
            return true;
          } else if (AclOperation.CREATE.equals(dto.getOp())) {
            authzAclService.createAcl(dto.getAcl());
            return true;
          }
        } catch (FeignException e) {
          log.warn("can't update acl", e);
          try {
            Thread.sleep(5000L);
          } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    }

    return false;
  }

  private void getCheckRequestAndDelay(Jedis conn) {
    List<String> data = conn.blpop(0, CHECK_REQUEST_QUEUE_NAME);
    if (data != null) {
      String payload = data.get(1);
      CheckTestDto dto;
      try {
        dto = mapper.readValue(payload, CheckTestDto.class);
        dq.add(new DCheckTestDto(dto, DELAY_TIME));
      } catch (JsonProcessingException e) {
        log.warn("Can't deserialize check request");
      }
    }
  }
}
