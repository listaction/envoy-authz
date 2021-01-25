package org.example.authserver.service;


import lombok.extern.slf4j.Slf4j;

import authserver.acl.Acl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CacheService {

    protected final Map<String, List<Acl>> resourceCache = new ConcurrentHashMap<>();
    protected final Map<Pattern, List<Acl>> patternCache = new ConcurrentHashMap<>();
    protected final Map<String, Long> urlCacheTime = new ConcurrentHashMap<>();

    private final Integer cacheDurationSeconds;

    public CacheService(@Value("${app.cache.duration:100}") Integer cacheDurationSeconds) {
        this.cacheDurationSeconds = cacheDurationSeconds;
    }

    public List<Acl> getFromUrlCache(String url) {
        List<Acl> result;
        result = resourceCache.get(url);

        if (!urlCacheCheckExpiration(url)) {
            result = null;
        }

        if (result == null) {
            result = getFromPatternCache(url);
            saveUrlCache(url, result);
        }

        return result;
    }

    protected void saveUrlCache(String url, List<Acl> result) {
        resourceCache.put(url, result);
        urlCacheTime.put(url, System.currentTimeMillis());
    }

    protected boolean urlCacheCheckExpiration(String url) {
        return urlCacheCheckExpiration(url, cacheDurationSeconds);
    }

    protected boolean urlCacheCheckExpiration(String url, int duration) {
        Long cacheTime = urlCacheTime.get(url);
        if (cacheTime == null) return false;

        if (cacheTime + duration * 1000L < System.currentTimeMillis()) {
            resourceCache.remove(url);
            urlCacheTime.remove(url);
            return false;
        }
        return true;
    }


    public List<Acl> getFromPatternCache(String url) {
        List<Acl> result = new ArrayList<>();
        patternCache.forEach((pattern, list) -> {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                result.addAll(list);
            }
        });

        return result;
    }

    public void putToPatternCache(Pattern pattern, Acl acl) {
        List<Acl> acls = new ArrayList<>();
        Pattern patternInCache = null;
        for (Map.Entry<Pattern, List<Acl>> entry : patternCache.entrySet()) {
            if (entry.getKey().toString().equalsIgnoreCase(pattern.toString())) {
                acls = entry.getValue();
                patternInCache = entry.getKey();
                break;
            }
        }

        if (patternInCache == null) {
            List<Acl> exp = new ArrayList<>();
            exp.add(acl);
            updatePattern(pattern, exp);
        } else {

            int index = acls.indexOf(acl);
            if (index >= 0) {
                Acl oldAcl = acls.get(index);
                if (!oldAcl.equals(acl)) {
                    acls.remove(oldAcl);
                    acls.add(acl);
                    updatePattern(pattern, acls);
                    log.debug("Pattern cache updating for pattern {}", pattern);
                }

            } else {
                acls.add(acl);
                updatePattern(pattern, acls);
                log.debug("Pattern cache adding for pattern {}", pattern);
            }

        }

    }

    private void updatePattern(Pattern pattern, List<Acl> acls) {
        for (Map.Entry<Pattern, List<Acl>> entry : patternCache.entrySet()) {
            if (entry.getKey().toString().equalsIgnoreCase(pattern.toString())) {
                patternCache.put(entry.getKey(), acls);
                return;
            }
        }
        patternCache.put(pattern, acls);
    }

    public void removeFromPatternCache(Pattern pattern, Acl acl) {
        List<Acl> acls = new ArrayList<>();
        Pattern patternInCache = null;
        for (Map.Entry<Pattern, List<Acl>> entry : patternCache.entrySet()) {
            if (entry.getKey().toString().equalsIgnoreCase(pattern.toString())) {
                acls = entry.getValue();
                patternInCache = entry.getKey();
                break;
            }
        }
        if (patternInCache != null) {
            acls.remove(acl);
            updatePattern(patternInCache, acls);
        }
    }

    public void removeFromCache(Acl experiment) {
        for (Map.Entry<Pattern, List<Acl>> entry : patternCache.entrySet()) {
            List<Acl> acls = entry.getValue();
            acls.remove(experiment);
        }
        for (Map.Entry<String, List<Acl>> entry : resourceCache.entrySet()) {
            List<Acl> acls = entry.getValue();
            acls.remove(experiment);
        }
    }

    public void cleanExpiredUrlCache() {
        urlCacheTime.forEach((url, time) -> {
            if (!urlCacheCheckExpiration(url)) {
                log.debug("Cleaned expired url cache {}", url);
                resourceCache.remove(url);
            }
        });
    }
}
