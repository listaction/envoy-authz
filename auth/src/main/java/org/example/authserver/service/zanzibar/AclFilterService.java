package org.example.authserver.service.zanzibar;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AclFilterService {

    private final Zanzibar zanzibar;
    private final ObjectMapper mapper;

    public boolean isAllowed(CheckRequest request) {
        String method;
        String path;
        String token;
        String objectNamespace;
        String serviceNamespace;
        String servicePath;
        Map<String, String> route;

        String relation;
        String objectId;

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
            route = matcher.matchAndExtract(servicePath, matcher.parseRoute(path));
            log.info("route: {}", route);
            Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
            token = headers.get("authorization");

            relation = request.getAttributes().getContextExtensionsOrThrow("relation");
            objectId = route.get("objectId");

            String objectIdPtr = request.getAttributes().getContextExtensionsMap().get("objectid_ptr");
            if (objectId == null && objectIdPtr != null) {
                byte[] requestBody = request.getAttributes().getRequest().getHttp().getBodyBytes().toByteArray();
                if (requestBody != null) {
                    ObjectNode nodes = mapper.readValue(requestBody, ObjectNode.class);
                    JsonNode objectIdNode = nodes.at(objectIdPtr);
                    objectId = objectIdNode.asText();
                }
            }

        } catch (NullPointerException npe) {
            log.warn("Can't parse request's headers {}", request, npe);
            return false;
        } catch (IOException e) {
            log.warn("Can't parse request body {}", request, e);
            return false;
        }
        if (token == null) return false;
        token = token.replace("Bearer ", "");
        log.info("Token: {}", token);

        log.info("[{}] CHECKING: {}:{}#{}@{}", method, objectNamespace, objectId, relation, token);

        if (!zanzibar.check(serviceNamespace, objectNamespace, "enable", token)) {
            return false;
        }

        return zanzibar.check(objectNamespace, objectId != null ? objectId : "null", relation, token);
    }

}