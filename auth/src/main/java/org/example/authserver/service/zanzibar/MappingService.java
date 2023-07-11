package org.example.authserver.service.zanzibar;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.example.authserver.entity.*;
import org.example.authserver.repo.MappingRepository;
import org.example.authserver.service.MappingCacheService;
import org.example.authserver.util.pattern.PathPatternParser;
import org.example.authserver.util.pattern.PathPatternRouteMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class MappingService {

  private final MappingCacheService mappingCacheService;

  private final MappingRepository mappingRepository;

  public MappingService(
      MappingCacheService mappingCacheService, MappingRepository mappingRepository) {
    this.mappingCacheService = mappingCacheService;
    this.mappingRepository = mappingRepository;
  }

  /**
   * @return mapping variables, or {@code null} for no match
   */
  public List<Mapping> processRequest(
      String requestMethod,
      String requestPath,
      Map<String, String> headersMap,
      String userId,
      String tenant) {
    String path = removeQuery(requestPath);
    Map<MappingEntity, Map<String, String>> mappings = findMappings(requestMethod, path);

    if (CollectionUtils.isEmpty(mappings)) { // no match
      return null;
    }

    List<Mapping> result = new ArrayList<>();

    for (Map.Entry<MappingEntity, Map<String, String>> entry : mappings.entrySet()) {
      MappingEntity mappingEntity = entry.getKey();

      Mapping mapping = new Mapping(entry.getKey());
      mapping.getVariableMap().putAll(entry.getValue());

      if (mappingEntity.getHeaderMapping() != null) {
        mapping.getVariableMap().putAll(parseHeaders(mappingEntity.getHeaderMapping(), headersMap));
      }

      mapping.getVariableMap().put("tenant", tenant);
      mapping.getVariableMap().put("userId", userId);
      mapping.getVariableMap().put("aclId", mappingEntity.getId());
      mapping.getVariableMap().forEach((key, v) -> log.trace("{} => {}", key, v));

      compositeAclStuff(mappingEntity, mapping.getVariableMap());
      result.add(mapping);
    }

    return result;
  }

  public Map<MappingEntity, Map<String, String>> findMappings(String requestMethod, String path) {
    List<MappingEntity> mappings = mappingCacheService.getAll();
    Map<MappingEntity, Map<String, String>> result = new HashMap<>();

    for (MappingEntity currentMapping : mappings) {
      Map<String, String> route;
      String mappingMethod = currentMapping.getMethod().name();
      if (!requestMethod.equalsIgnoreCase(mappingMethod)) {
        continue; // skip entries that don't match
      }

      PathPatternParser parser = new PathPatternParser();
      PathPatternRouteMatcher matcher = new PathPatternRouteMatcher(parser);
      route = matcher.matchAndExtract(currentMapping.getPath(), matcher.parseRoute(path));

      if (route == null) {
        continue; // skip entries that don't match
      }

      log.debug("route: {}", route);

      Map<String, String> pathVariables = new HashMap<>();
      for (Map.Entry<String, String> entry : route.entrySet()) {
        pathVariables.put(
            "path." + entry.getKey(), entry.getValue()); // transform path X variable to path.X
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

  private Map<String, String> parseHeaders(
      List<HeaderMappingKey> headerMapping, Map<String, String> headersMap) {
    Map<String, String> variables = new HashMap<>();
    for (HeaderMappingKey h : headerMapping) {
      variables.put("header." + h.getNamespace(), headersMap.get(h.getName()));
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
