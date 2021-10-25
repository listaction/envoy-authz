package org.example.authserver.service.model;

import authserver.acl.Acl;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.authserver.service.zanzibar.ZanzibarImpl;
import reactor.util.function.Tuple2;

import java.util.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RequestCache {

    private Map<Tuple2<String, String>, Set<ZanzibarImpl.ExpandedAcl>> cache = new HashMap<>();
    private Map<String, Set<Acl>> principalAclCache = new HashMap<>();
    private Map<String, Set<String>> principalHighCardinalityCache = new HashMap<>();

    public Acl getMaxAcl(String user) {
        Set<Acl> acls = principalAclCache.get(user);
        if (acls == null || acls.isEmpty())
            return null;
        return Collections.max(acls, Comparator.comparingLong(Acl::getUpdated));
    }

    public long getMaxAclUpdated(String user) {
        Acl maxAcl = getMaxAcl(user);
        return maxAcl != null ? maxAcl.getUpdated() : 0;
    }
}
