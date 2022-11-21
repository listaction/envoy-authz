package org.example.authserver.service;

import authserver.common.AclOperationDto;
import authserver.common.CheckTestDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.config.AppProperties;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Slf4j
@Service
public class SplitTestService {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Executor executor = Executors.newFixedThreadPool(1);
  private static final String CHECK_REQUEST_QUEUE_NAME = "CheckQueue";
  private final String ACL_QUEUE_NAME = "AclQueue";

  private final JedisPool jedisPool;
  private final AppProperties appProperties;
  private final boolean splitTestEnabled;

  public SplitTestService(JedisPool jedisPool, AppProperties appProperties) {
    this.jedisPool = jedisPool;
    this.appProperties = appProperties;
    this.splitTestEnabled = appProperties.isCopyMode();
  }

  public void submitAsync(AclOperationDto dto) {
    if (!splitTestEnabled) return;
    executor.execute(() -> submit(dto));
  }

  public void submitAsync(CheckTestDto dto) {
    if (!splitTestEnabled) return;
    executor.execute(() -> submit(dto));
  }

  private void submit(CheckTestDto dto) {
    log.info("Submit data to check queue: {}", dto);
    String json = null;
    try {
      json = mapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      log.warn("Can't serialize dto: {}", dto, e);
    }

    if (json == null) return;

    try (Jedis conn = jedisPool.getResource()) {
      conn.rpush(CHECK_REQUEST_QUEUE_NAME, json);
      conn.expire(CHECK_REQUEST_QUEUE_NAME, 600);
    }
  }

  private void submit(AclOperationDto dto) {
    log.info("Submit data to acl queue: {}", dto);
    String json = null;
    try {
      json = mapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      log.warn("Can't serialize dto: {}", dto, e);
    }

    if (json == null) return;

    try (Jedis conn = jedisPool.getResource()) {
      conn.rpush(ACL_QUEUE_NAME, json);
      conn.expire(ACL_QUEUE_NAME, 4 * 3600);
    }
  }
}
