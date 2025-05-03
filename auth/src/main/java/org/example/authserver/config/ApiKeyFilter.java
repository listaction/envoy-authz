package org.example.authserver.config;

import com.google.common.base.Strings;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.example.authserver.controller.MappingController;
import org.springframework.http.HttpStatus;

public class ApiKeyFilter implements Filter {
  private final String apiKey;
  private final boolean protectionMode;

  public ApiKeyFilter(AppProperties properties) {
    this.apiKey = properties.getApiKey();
    this.protectionMode = properties.isApiProtectionMode();
    if (protectionMode && Strings.isNullOrEmpty(apiKey)) {
      throw new IllegalStateException("Empty api key");
    }
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    String requestApiKey = ((HttpServletRequestImpl) request).getHeader(MappingController.API_KEY);

    if (protectionMode && !Objects.equals(apiKey, requestApiKey)) {
      httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
    } else {
      chain.doFilter(request, response);
    }
  }
}
