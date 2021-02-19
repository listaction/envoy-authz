package org.example.authserver;

import authserver.acl.Acl;
import authserver.acl.AclRelationConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Utils {

    private static final ObjectMapper mapper = new ObjectMapper();

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

}
