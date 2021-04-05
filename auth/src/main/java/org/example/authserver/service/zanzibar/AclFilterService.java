package org.example.authserver.service.zanzibar;

import com.google.common.base.Strings;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AclFilterService {

    private final Zanzibar zanzibar;
    private final MappingService mappingService;
    private final TokenService tokenService;

    public AclFilterService(Zanzibar zanzibar, MappingService mappingService, TokenService tokenService) {
        this.zanzibar = zanzibar;
        this.mappingService = mappingService;
        this.tokenService = tokenService;
    }

    public boolean isAllowed(CheckRequest request){
        Claims claims = tokenService.getAllClaimsFromRequest(request);
        if (claims == null) return false;

        List<Map<String, String>> mappings = mappingService.processRequest(request, claims);
        if (mappings == null || mappings.size() == 0){
            return false;
        }

        for (Map<String, String> variables : mappings){

            String mappingRoles = variables.getOrDefault("roles", "");
            List<String> mRoles;
            if (!Strings.isNullOrEmpty(mappingRoles)){
                String[] tmp = mappingRoles.split(",");
                mRoles = Arrays.asList(tmp);
            } else {
                log.info("No roles assigned");
                return false;
            }

            boolean r = false;
            for (String role : mRoles) {
                log.info("CHECKING: {}:{}#{}@{}", variables.get("namespace"), variables.get("object"), role, claims.getSubject());
                boolean check = zanzibar.check(variables.get("namespace"), variables.get("object"), role, claims.getSubject());
                if (check) r = true;
            }
            if (!r) return false;
        }

        return true;
    }

}
