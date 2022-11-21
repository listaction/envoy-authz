package com.example.splittest.service;

import authserver.common.AclOperation;
import authserver.common.AclOperationDto;
import authserver.common.CheckTestDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
@Component
public class QueueListener {

  private final ObjectMapper mapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final String CHECK_REQUEST_QUEUE_NAME = "CheckQueue";
  private final String ACL_QUEUE_NAME = "AclQueue";
  private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
  private final AuthzTestService authzTestService;
  private final AuthzAclService authzAclService;
  private final JedisPool jedisPool;

  public QueueListener(
      AuthzTestService authzTestService, AuthzAclService authzAclService, JedisPool jedisPool) {
    this.authzTestService = authzTestService;
    this.authzAclService = authzAclService;
    this.jedisPool = jedisPool;

    subscribeChanges();
  }

  private void subscribeChanges() {
    executor.execute(this::getAclModifications);
    executor.execute(this::getCheckRequestAndTest);
  }

  private void getAclModifications() {
    while (!Thread.interrupted()) {
      try (Jedis conn = jedisPool.getResource()) {
        List<String> data = conn.blpop(0, ACL_QUEUE_NAME);
        if (data != null) {
          String payload = data.get(1);
          AclOperationDto dto = mapper.readValue(payload, AclOperationDto.class);
          if (AclOperation.DEL.equals(dto.getOp())) {
            authzAclService.deleteAcl(dto.getAcl());
          } else if (AclOperation.CREATE.equals(dto.getOp())) {
            authzAclService.createAcl(dto.getAcl());
          }
        }
      } catch (Exception e) {
        log.warn("Can't process acl", e);
      }
    }
  }

  private void getCheckRequestAndTest() {
    while (!Thread.interrupted()) {
      try (Jedis conn = jedisPool.getResource()) {
        List<String> data = conn.blpop(0, CHECK_REQUEST_QUEUE_NAME);
        if (data != null) {
          String payload = data.get(1);
          CheckTestDto dto = mapper.readValue(payload, CheckTestDto.class);
          authzTestService.authzTest(dto);
        }
      } catch (Exception e) {
        log.warn("Can't process check", e);
      }
    }
  }
}
