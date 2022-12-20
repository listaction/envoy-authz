package com.example.splittest.config;

import authserver.acl.Acl;
import com.example.splittest.entity.CheckResult;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AuthzClient {

  @RequestLine("POST /acl/create")
  @Headers({"Content-Type: application/json", "Authorization: {authorization}"})
  void createAcl(@Param("authorization") String authHeader, Acl acl);

  @RequestLine("POST /acl/delete")
  @Headers({"Content-Type: application/json", "Authorization: {authorization}"})
  void deleteAcl(@Param("authorization") String authHeader, Acl acl);

  @RequestLine("GET /debug/query?q={query}")
  @Headers({"Content-Type: application/json", "Authorization: {authorization}"})
  CheckResult checkQuery(@Param("authorization") String authHeader, @Param("query") String query);
}
