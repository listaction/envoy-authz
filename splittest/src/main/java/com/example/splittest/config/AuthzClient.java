package com.example.splittest.config;

import authserver.acl.Acl;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AuthzClient {

  @RequestLine("POST /acl/create")
  @Headers({"Content-Type: application/json", "Authorization: {authorization}"})
  void createAcl(@Param("authorization") String authHeader, Acl acl);

  @RequestLine("POST /acl/create_multiple")
  @Headers({"Content-Type: application/json", "Authorization: {authorization}"})
  void deleteAcl(@Param("authorization") String authHeader, Acl acl);
}
