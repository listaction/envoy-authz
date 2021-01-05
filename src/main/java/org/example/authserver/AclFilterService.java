package org.example.authserver;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AclFilterService {

    private final CacheService cacheService;

    public AclFilterService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public boolean isAllowed(CheckRequest request){
        String path;
        String token;
        try {
            path = request.getAttributes().getRequest().getHttp().getPath();
            Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
            token = headers.get("authorization");
        } catch (NullPointerException npe){
            log.warn("Can't parse request's headers {}", request, npe);
            return false;
        }
        if (token == null) return false;
        token = token.replace("Bearer ", "");

        List<Acl> aclList = cacheService.getFromUrlCache(path);
        if (aclList == null || aclList.size() == 0){
            aclList = cacheService.getFromPatternCache(path);
            if (aclList.size() == 0) return false;
        }

        for (Acl acl : aclList){
            if (!token.equals(acl.getToken())) continue; // skip another tokens

            if (acl.getAllow()) return true;
        }

        return false;
    }

}
