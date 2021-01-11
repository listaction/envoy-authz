package org.example.authserver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.authserver.domain.Acl;

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

    public static String aclToJson(Acl acl){
        try {
            return mapper.writeValueAsString(acl);
        } catch (JsonProcessingException e) {
            log.warn("Can't serialize ACL: {}", acl, e);
            return null;
        }
    }
}
