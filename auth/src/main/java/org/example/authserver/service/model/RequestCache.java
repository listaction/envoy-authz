package org.example.authserver.service.model;

import authserver.acl.Acl;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.authserver.service.zanzibar.ZanzibarImpl;
import reactor.util.function.Tuple2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RequestCache {

    private Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache = new HashMap<>();
    private Map<String, Set<Acl>> principalAclCache = new HashMap<>();
}
