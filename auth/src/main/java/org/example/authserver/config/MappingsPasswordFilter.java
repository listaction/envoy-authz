package org.example.authserver.config;

import com.google.common.base.Strings;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Objects;

public class MappingsPasswordFilter implements Filter {
    private final String password;
    private final boolean protectionMode;

    public MappingsPasswordFilter(AppProperties properties) {
        this.password = properties.getMappingPassword();
        this.protectionMode = properties.isMappingProtectionMode();
        if (protectionMode && Strings.isNullOrEmpty(password)) {
            throw new IllegalStateException("Empty mappings password");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String passwordHeader = ((HttpServletRequestImpl) request).getHeader("X-MAPPINGS-PASSWORD");

        if (protectionMode && !Objects.equals(password, passwordHeader)) {
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        } else {
            chain.doFilter(request, response);
        }
    }
}





