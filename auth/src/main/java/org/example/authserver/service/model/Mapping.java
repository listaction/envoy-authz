package org.example.authserver.service.model;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Mapping {

    private Map<String, String> map = new HashMap<>();

    public String get(String key) {
        return map.get(key);
    }

    public Set<String> parseRoles() {
        Set<String> roles = new HashSet<>();

        String mappingRoles = map.getOrDefault("roles", "");
        if (!Strings.isNullOrEmpty(mappingRoles)) {
            String[] tmp = mappingRoles.split(",");
            roles.addAll(Arrays.asList(tmp));
        }
        return roles;
    }
}
