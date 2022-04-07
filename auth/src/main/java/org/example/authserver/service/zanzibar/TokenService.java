package org.example.authserver.service.zanzibar;

import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtHandlerAdapter;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TokenService {

    private static final String AUTH_HEADER = "authorization";

    public Claims getAllClaimsFromToken(String token) {
        Claims claims;
        try {
            claims = parseClaimsJws(token).getBody();
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }

    public Claims getAllClaimsFromRequest(CheckRequest request) {
        String token = getToken(request);
        if (token == null) return null;
        return getAllClaimsFromToken(token);
    }

    public String getToken(CheckRequest request) {
        Map<String, String> headers = request.getAttributes().getRequest().getHttp().getHeadersMap();
        String authHeader = headers.get(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private Jws<Claims> parseClaimsJws(String claimsJws) {
        UnverifiedJwtParser parser = new UnverifiedJwtParser();
        return parser.parse(claimsJws, new JwtHandlerAdapter<Jws<Claims>>() {
            @Override
            public Jws<Claims> onClaimsJws(Jws<Claims> jws) {
                return jws;
            }
        });
    }

}
