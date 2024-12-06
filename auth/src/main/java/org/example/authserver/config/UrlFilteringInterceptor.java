package org.example.authserver.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class UrlFilteringInterceptor implements HandlerInterceptor {

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    StringBuffer url = request.getRequestURL();
    url.delete(0, 7); // http://
    if (url.indexOf("//") != -1) {
      log.error("Double slash detected, blocking url {}", request.getRequestURL());
      response.sendError(400, "Double slash in URL is prohibited");
      return false;
    }

    return true;
  }
}
