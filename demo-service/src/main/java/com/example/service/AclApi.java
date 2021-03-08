package com.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import authserver.acl.Acl;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
@Service
public class AclApi {
    private static final OkHttpClient client = new OkHttpClient();
    private final static ObjectMapper mapper = new ObjectMapper();
    private final static String ACL_REDIS_KEY = "acls";

    private final String authZAddress;

    public AclApi(@Value("${authz.address}") String authZAddress) {
        this.authZAddress = authZAddress;
    }

    public void addRule(Acl acl) throws IOException {
        String json = aclToJson(acl);

        String url = authZAddress+"/acl/create";

        Request request = new Request.Builder()
                .header("Authorization", "Bearer acl_admin")
                .header("Accept", "application/json")
                .method("POST", RequestBody.create(MediaType.get("application/json"), json))
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null){
                log.info("body is null");
                throw new IOException(url);
            }
        }
    }

    public static String aclToJson(Acl acl){
        try {
            return mapper.writeValueAsString(acl);
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize ACL: {}", acl, e);
            return null;
        }
    }
}
