package org.example.authserver.service.zanzibar;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.example.authserver.entity.BodyMapping;
import org.example.authserver.entity.BodyMappingKey;
import org.example.authserver.entity.HeaderMappingKey;
import org.example.authserver.entity.MappingEntity;
import org.example.authserver.repo.pgsql.MappingRepository;
import org.example.authserver.service.MappingCacheService;
import org.example.authserver.service.model.Mapping;
import org.springframework.stereotype.Service;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MappingService {

    private final static Pattern pattern = Pattern.compile("(.*)\\/realms\\/(.*)");

    private final MappingCacheService mappingCacheService;

    private final MappingRepository mappingRepository;

    public MappingService(MappingCacheService mappingCacheService, MappingRepository mappingRepository) {
        this.mappingCacheService = mappingCacheService;
        this.mappingRepository = mappingRepository;
    }


    /**
     * @return mapping variables, or {@code null} for no match
     */
    public List<Mapping> processRequest(CheckRequest request, Claims claims) {
        Map<MappingEntity, Map<String, String>> mappings = findMappings(request);
        if (mappings == null) return null; // no match

        List<Mapping> result = new ArrayList<>();

        String requestMethod = request.getAttributes().getRequest().getHttp().getMethod();
        Map<String, String> headersMap = request.getAttributes().getRequest().getHttp().getHeadersMap();

        for (Map.Entry<MappingEntity, Map<String, String>> entry : mappings.entrySet()) {
            MappingEntity mappingEntity = entry.getKey();

            Mapping mapping = new Mapping(entry.getKey());
            mapping.getVariableMap().putAll(entry.getValue());

            if (mappingEntity.getBodyMapping() != null && ("POST".equalsIgnoreCase(requestMethod) || "PUT".equalsIgnoreCase(requestMethod))) {
                String requestBody = request.getAttributes().getRequest().getHttp().getBody();
                BodyMapping bodyMapping = mappingEntity.getBodyMapping();
                mapping.getVariableMap().putAll(parseRequestJsonBody(bodyMapping, requestBody));
            }

            if (mappingEntity.getHeaderMapping() != null) {
                mapping.getVariableMap().putAll(parseHeaders(mappingEntity.getHeaderMapping(), headersMap));
            }

            Matcher m = pattern.matcher(claims.getIssuer());
            if (m.matches() && m.groupCount() >= 2) {
                mapping.getVariableMap().put("tenant", m.group(2));
            }

            mapping.getVariableMap().put("userId", claims.getSubject());
            mapping.getVariableMap().put("aclId", mappingEntity.getId());
            mapping.getVariableMap().forEach((key, v) -> log.trace("{} => {}", key, v));

            compositeAclStuff(mappingEntity, mapping.getVariableMap());
            result.add(mapping);
        }

        return result;
    }

    public Map<MappingEntity, Map<String, String>> findMappings(CheckRequest request) {
        List<MappingEntity> mappings = mappingCacheService.getAll();
        Map<MappingEntity, Map<String, String>> result = new HashMap<>();

        String requestMethod = request.getAttributes().getRequest().getHttp().getMethod();

        for (MappingEntity currentMapping : mappings) {
            Map<String, String> route;
            String mappingMethod = currentMapping.getMethod().name();
            if (!requestMethod.equalsIgnoreCase(mappingMethod)) {
                continue; // skip entries that don't match
            }

            String path = removeQuery(request.getAttributes().getRequest().getHttp().getPath());
            PathPatternParser parser = new PathPatternParser();
            PathPatternRouteMatcher matcher = new PathPatternRouteMatcher(parser);
            route = matcher.matchAndExtract(currentMapping.getPath(), matcher.parseRoute(path));

            if (route == null) {
                continue; // skip entries that don't match
            }

            log.debug("route: {}", route);

            Map<String, String> pathVariables = new HashMap<>();
            for (Map.Entry<String, String> entry : route.entrySet()) {
                pathVariables.put("path." + entry.getKey(), entry.getValue()); // transform path X variable to path.X
            }

            result.put(currentMapping, pathVariables);
        }

        return result;
    }

    public MappingEntity create(MappingEntity mappingEntity) {
        mappingEntity.setId(UUID.randomUUID().toString());
        return mappingRepository.save(mappingEntity);
    }

    public List<MappingEntity> findAll() {
        return mappingRepository.findAll();
    }

    public void deleteAll() {
        mappingRepository.deleteAll();
    }

    public void deleteById(String id) {
        mappingRepository.deleteById(id);
    }

    public void notifyAllToRefreshCache() {
        mappingCacheService.notifyAllToRefreshCache();
    }

    private Map<String, String> parseHeaders(List<HeaderMappingKey> headerMapping, Map<String, String> headersMap) {
        Map<String, String> variables = new HashMap<>();
        for (HeaderMappingKey h : headerMapping) {
            variables.put("body." + h.getNamespace(), headersMap.get(h.getName()));
        }

        return variables;
    }

    private void compositeAclStuff(MappingEntity mapping, Map<String, String> variables) {
        String namespace = StringSubstitutor.replace(mapping.getNamespace(), variables, "{", "}");
        String object = StringSubstitutor.replace(mapping.getObject(), variables, "{", "}");

        variables.put("namespace", namespace);
        variables.put("object", object);
    }

    private Map<String, String> parseRequestJsonBody(BodyMapping bodyMapping, String requestBody) {
        Map<String, String> variables = new HashMap<>();
        DocumentContext jsonContext = JsonPath.parse(requestBody);
        for (BodyMappingKey k : bodyMapping.getKeys()) {
            String value = jsonContext.read(k.getXpath());
            variables.put("body." + k.getNamespace(), value);
        }

        return variables;
    }

    private static String removeQuery(String pathOrig) {
        int idx = pathOrig.indexOf('?');
        if (idx == -1) return pathOrig;
        return pathOrig.substring(0, idx);
    }
}
