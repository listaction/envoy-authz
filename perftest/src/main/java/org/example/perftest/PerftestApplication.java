package org.example.perftest;

import authserver.acl.Acl;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class PerftestApplication implements CommandLineRunner {

  private static final OkHttpClient client = new OkHttpClient();

  private final AclApi api;
  private final String testAddress;
  private final int testParallel;
  private final String prometheusAddress;
  private final List<String> ids = new CopyOnWriteArrayList<>();
  private final int testCount;
  private final String testId;
  private final ThreadPoolExecutor executor;
  private volatile CountDownLatch countDownLatch;

  public PerftestApplication(
      AclApi api,
      @Value("${test.address}") String testAddress,
      @Value("${test.parallel:1}") int testParallel,
      @Value("${test.count:1}") int testCount,
      @Value("${prometheus.address}") String prometheusAddress) {
    this.api = api;
    this.testAddress = testAddress;
    this.testParallel = testParallel;
    this.testCount = testCount;
    this.prometheusAddress = prometheusAddress;
    this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(testParallel);
    this.testId = "TEST_" + UUID.randomUUID().toString();
  }

  public static void main(String[] args) {
    SpringApplication.run(PerftestApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("testId: {}", testId);
    createAcls();
    countDownLatch = new CountDownLatch(ids.size());
    log.info("testing in {} threads", testParallel);
    ScheduledExecutorService sch = Executors.newSingleThreadScheduledExecutor();
    sch.scheduleAtFixedRate(
        () -> log.info("tests in progress: {}", countDownLatch.getCount()),
        10,
        10,
        TimeUnit.SECONDS);
    testIds();
    log.info("results:");
    showResults();
    System.exit(0);
  }

  private void showResults() throws IOException {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(prometheusAddress).newBuilder();
    String url = urlBuilder.build().toString();

    Request request = new Request.Builder().url(url).build();

    try (Response response = client.newCall(request).execute()) {
      ResponseBody body = response.body();
      if (body == null) {
        throw new IOException(url);
      }
      String data = new String(body.bytes());
      String[] lines = data.split("\n");
      for (String line : lines) {
        if (!line.startsWith("checkAcl_")) continue;
        log.info("{}", line);
      }
    }
  }

  private void testIds() throws InterruptedException {
    for (int i = 0; i < ids.size(); i++) {
      String userId = ids.get(i);
      executor.execute(
          () -> {
            try {
              doTestRequest(userId, countDownLatch);
            } catch (IOException e) {
              log.warn("Can't process check request for user {}", userId);
              countDownLatch.countDown();
            }
          });
    }

    countDownLatch.await();
  }

  private void doTestRequest(String userId, CountDownLatch signal) throws IOException {
    HttpUrl.Builder urlBuilder = HttpUrl.parse(testAddress).newBuilder();
    urlBuilder.addQueryParameter("namespace", "contact");
    urlBuilder.addQueryParameter("object", testId);
    urlBuilder.addQueryParameter("principal", userId);
    urlBuilder.addQueryParameter("relation", "viewer");
    String url = urlBuilder.build().toString();

    Request request = new Request.Builder().header("Accept", "application/json").url(url).build();

    try (Response response = client.newCall(request).execute()) {
      ResponseBody body = response.body();
      if (body == null) {
        throw new IOException(url);
      }
      String r = new String(body.bytes());
      if (!r.equals("true")) {
        log.warn("Unexpected response for user: {} => {}", userId, r);
      }
    }

    signal.countDown();
  }

  private void createAcls() throws IOException {
    List<String> roles = new ArrayList<>();
    roles.add("editor");
    roles.add("viewer");
    roles.add("member");
    roles.add("admin");
    Acl acl = Acl.create(String.format("contact:%s#editor@group:contactusers#member", testId));
    api.addRule(acl);
    for (int i = 0; i < testCount; i++) {
      if (i % 100 == 0) {
        log.info("creating rules... {}", i);
      }
      Collections.shuffle(roles);
      String role = roles.get(0);
      String id = UUID.randomUUID().toString();
      Acl aclUser = Acl.create(String.format("group:contactusers#%s@%s", role, id));
      for (int retry = 0; retry < 5; retry++) {
        try {
          api.addRule(aclUser);
          break;
        } catch (IOException e) {
          log.warn("Can't add rule: {}", aclUser.toString());
        }
      }

      ids.add(id);
    }
  }
}
