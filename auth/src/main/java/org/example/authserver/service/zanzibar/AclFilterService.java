package org.example.authserver.service.zanzibar;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class AclFilterService {

    private final Zanzibar zanzibar;

    public AclFilterService(Zanzibar zanzibar) {
        this.zanzibar = zanzibar;
    }

    public boolean isAllowed(CheckRequest request){
        String method;
        String path;
        String token;
        String objectNamespace;
        String serviceNamespace;
        String servicePath;
        Map<String, String> route;
        try {
            path = request.getAttributes().getRequest().getHttp().getPath();
            objectNamespace = request.getAttributes().getContextExtensionsOrThrow("namespace_object");
            serviceNamespace = request.getAttributes().getContextExtensionsOrThrow("namespace_service");
            servicePath = request.getAttributes().getContextExtensionsOrThrow("service_path");
            log.info("path: {}", path);
            method = request.getAttributes().getRequest().getHttp().getMethod();
            log.info("method: {}", method);
            PathPatternParser parser = new PathPatternParser();
            PathPatternRouteMatcher matcher = new PathPatternRouteMatcher(parser);
            route  = matcher.matchAndExtract(servicePath, matcher.parseRoute(path));
            log.info("route: {}", route);
            Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
            token = headers.get("authorization");
        } catch (NullPointerException npe){
            log.warn("Can't parse request's headers {}", request, npe);
            return false;
        }
        if (token == null) return false;
        if (route == null) route = new HashMap<>();
        token = token.replace("Bearer ", "");
        log.info("Token: {}", token);

        String relation;
        switch (method.toLowerCase(Locale.getDefault())){
            case "post":
                relation = "owner";
                break;
            case "put":
                relation = "editor";
                break;
            case "delete":
                relation = "owner";
                break;
            default:
                relation = "viewer";
        }

        log.info("[{}] CHECKING: {}:{}#{}@{}", method, objectNamespace, route.get("objectId"), relation, token);

        if (!zanzibar.check(serviceNamespace, objectNamespace, "enable", token)){
            return false;
        }

        return zanzibar.check(objectNamespace, route.getOrDefault("objectId", "null"), relation, token);
    }

}
