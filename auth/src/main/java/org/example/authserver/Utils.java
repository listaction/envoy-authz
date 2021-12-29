package org.example.authserver;

import authserver.acl.Acl;
import authserver.acl.AclRelationConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class Utils {

    private static final Pattern TAG_REGEX_PATTERN = Pattern.compile("(\\S+):(\\S+)#(\\S+)");
    private static final ObjectMapper mapper = new ObjectMapper();

    private Utils() {
    }

    public static String createTag(String namespace, String object, String relation) {
        return String.format("%s:%s#%s", namespace, object, relation);
    }

    public static String createTag(String nsObject, String relation) {
        return String.format("%s#%s", nsObject, relation);
    }

    public static Acl jsonToAcl(String json){
        try {
            return mapper.readValue(json, Acl.class);
        } catch (JsonProcessingException e) {
            log.warn("Can't deserialize ACL: {}", json, e);
            return null;
        }
    }

    public static AclRelationConfig jsonToConfig(String json){
        try {
            return mapper.readValue(json, AclRelationConfig.class);
        } catch (JsonProcessingException e) {
            log.warn("Can't deserialize ACL: {}", json, e);
            return null;
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

    public static String configToJson(AclRelationConfig acl){
        try {
            return mapper.writeValueAsString(acl);
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize Config: {}", acl, e);
            return null;
        }
    }

    public static String createNsObject(String namespace, String object) {
        return namespace + ":" + object;
    }

    public static Acl parseTag(String tag){
        Matcher m = TAG_REGEX_PATTERN.matcher(tag);
        if (!m.find()) {
            log.warn(String.format("Can't parse ACL: %s", tag));
            return null;
        }

        String namespace = m.group(1);
        String object = m.group(2);
        String relation = m.group(3);

        return Acl.builder()
                .namespace(namespace)
                .object(object)
                .relation(relation)
                .build();
    }
}
