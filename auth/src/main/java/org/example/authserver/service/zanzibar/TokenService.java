package org.example.authserver.service.zanzibar;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtHandlerAdapter;
import java.util.List;
import java.util.Map;
import org.example.authserver.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TokenService {

  private static final String AUTH_HEADER = "authorization";

  private final AppProperties appProperties;

  public TokenService(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  public Claims getAllClaimsFromRequest(CheckRequest request) {
    String token = getTokenHeader(request);
    if (token == null && appProperties.isJwtParamEnabled()) {
      token = getTokenParam(request);
    }
    if (token == null) return null;
    return getAllClaimsFromToken(token);
  }

  public Claims getAllClaimsFromToken(String token) {
    Claims claims;
    try {
      claims = parseClaimsJws(token).getBody();
    } catch (Exception e) {
      claims = null;
    }
    return claims;
  }

  public String getTokenParam(CheckRequest request) {
    String path = request.getAttributes().getRequest().getHttp().getPath();
    return getTokenParam(path);
  }

  public String getTokenParam(String path) {
    MultiValueMap<String, String> queryParams =
        UriComponentsBuilder.fromUriString(path).build().getQueryParams();

    List<String> value = queryParams.get(appProperties.getJwtParam());
    if (value != null && value.size() > 0) {
      return value.get(0);
    }
    return null;
  }

  public String getTokenHeader(CheckRequest request) {
    Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
    String authHeader = headers.get(AUTH_HEADER);
    return getTokenHeader(authHeader);
  }

  public String getTokenHeader(String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7);
    }

    return null;
  }

  private Jws<Claims> parseClaimsJws(String claimsJws) {
    UnverifiedJwtParser parser = new UnverifiedJwtParser();
    return parser.parse(
        claimsJws,
        new JwtHandlerAdapter<Jws<Claims>>() {
          @Override
          public Jws<Claims> onClaimsJws(Jws<Claims> jws) {
            return jws;
          }
        });
  }
}
