package org.example.authserver.repo.redis;

import authserver.acl.Acl;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.Utils;
import org.example.authserver.repo.AclRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@ConditionalOnProperty(
        value="app.database",
        havingValue = "REDIS"
)
public class AclRedisRepository implements AclRepository {

    private static final String ACL_REDIS_KEY = "acls";
    private static final String IDX_NAMESPACE = "idx_namespace";
    private static final String IDX_USER = "idx_user";

    private final JedisPool jedis;

    public AclRedisRepository(JedisPool jedis) {
        this.jedis = jedis;
    }

    @Override
    public Set<Acl> findAll(){
        Jedis conn = jedis.getResource();
        Map<String, String> jsons = conn.hgetAll(ACL_REDIS_KEY);
        conn.close();
        return jsons.values().stream()
                .map(Utils::jsonToAcl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Acl findOneById(String id) {
        Jedis conn = jedis.getResource();
        Acl acl = findAcl(id, conn);
        conn.close();
        return acl;
    }

    @Override
    public Set<Acl> findAllByPrincipalAndNsObjectIn(String principal, List<String> nsObjects) {
        throw new RuntimeException("findAllByPrincipalAndNsObjectIn_REDIS_IS_NOT_IMPLEMENTED_YET");
    }

    @Override
    public Set<Acl> findAllByPrincipal(String principal) {
        throw new RuntimeException("findAllByPrincipal_REDIS_IS_NOT_IMPLEMENTED_YET");
    }

    @Override
    public Set<Acl> findAllByNsObjectIn(List<String> nsObjects) {
        throw new RuntimeException("findAllByNsObjectIn_REDIS_IS_NOT_IMPLEMENTED_YET");
    }

    @Override
    public Set<String> findAllEndUsers() {
        throw new RuntimeException("findAllByNsObjectIn_REDIS_IS_NOT_IMPLEMENTED_YET");
    }

    @Override
    public Set<String> findAllNamespaces() {
        throw new RuntimeException("findAllByNsObjectIn_REDIS_IS_NOT_IMPLEMENTED_YET");
    }

    @Override
    public Set<String> findAllObjects() {
        throw new RuntimeException("findAllByNsObjectIn_REDIS_IS_NOT_IMPLEMENTED_YET");
    }

    @Override
    public Set<Acl> findAllByNamespaceAndObjectAndUser(String namespace, String object, String user) {
        Set<Acl> acls = new HashSet<>();
        Jedis conn = jedis.getResource();
        ScanParams params = new ScanParams();
        params.match(String.format("%s:%s@*", namespace, object));
        ScanResult<Map.Entry<String, String>> scanResult = conn.hscan(IDX_NAMESPACE, "", params);
        for (Map.Entry<String, String> entry : scanResult.getResult()){
            acls.add(findAcl(entry.getValue(), conn));
        }
        conn.close();

        return acls.stream()
                .filter(f->(f.hasUserset() || (!f.hasUserset() && user.equals(f.getUser()))))
                .collect(Collectors.toSet());
    }

    private Acl findAcl(String value, Jedis conn) {
        String json = conn.hget(ACL_REDIS_KEY, value);
        return Utils.jsonToAcl(json);
    }

    public void save(Acl acl) {
        String json = Utils.aclToJson(acl);
        Jedis conn = jedis.getResource();
        conn.hset(ACL_REDIS_KEY, acl.getId().toString(), json);
        String namespace = String.format("%s:%s", acl.getNamespace(), acl.getObject());
        conn.hset("idx_namespace", namespace + "@" + acl.getId().toString(), acl.getId().toString());
        if (!acl.hasUserset()) {
            conn.hset("idx_user", acl.getUser() + "@" + acl.getId().toString(), acl.getId().toString());
        }
        conn.close();
    }

    public void delete(Acl acl){
        Jedis conn = jedis.getResource();
        conn.hdel(ACL_REDIS_KEY, acl.getId().toString());
        conn.publish(ACL_REDIS_KEY, acl.getId().toString());
        conn.close();
    }

}
