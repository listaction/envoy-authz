/*
 * Copyright (C) 2014 jsonwebtoken.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.authserver.service.zanzibar;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.*;
import io.jsonwebtoken.lang.Assert;
import io.jsonwebtoken.lang.Strings;

import java.io.IOException;
import java.security.Key;
import java.util.Map;

import static io.jsonwebtoken.JwtParser.SEPARATOR_CHAR;

@SuppressWarnings("unchecked")
public class UnverifiedJwtParser {

    //don't need millis since JWT date fields are only second granularity:
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    private ObjectMapper objectMapper = new ObjectMapper();

    private byte[] keyBytes;

    private Key key;

    private SigningKeyResolver signingKeyResolver;

    public Jwt parse(String jwt) throws ExpiredJwtException, MalformedJwtException, SignatureException {

        Assert.hasText(jwt, "JWT String argument cannot be null or empty.");

        String base64UrlEncodedHeader = null;
        String base64UrlEncodedPayload = null;
        String base64UrlEncodedDigest = null;

        int delimiterCount = 0;

        StringBuilder sb = new StringBuilder(128);

        for (char c : jwt.toCharArray()) {

            if (c == SEPARATOR_CHAR) {

                String token = Strings.clean(sb.toString());

                if (delimiterCount == 0) {
                    base64UrlEncodedHeader = token;
                } else if (delimiterCount == 1) {
                    base64UrlEncodedPayload = token;
                }

                delimiterCount++;
                sb = new StringBuilder(128);
            } else {
                sb.append(c);
            }
        }

        if (delimiterCount != 2) {
            String msg = "JWT strings must contain exactly 2 period characters. Found: " + delimiterCount;
            throw new MalformedJwtException(msg);
        }
        if (sb.length() > 0) {
            base64UrlEncodedDigest = sb.toString();
        }

        if (base64UrlEncodedPayload == null) {
            throw new MalformedJwtException("JWT string '" + jwt + "' is missing a body/payload.");
        }

        // =============== Header =================
        Header header = null;

        if (base64UrlEncodedHeader != null) {
            String origValue = TextCodec.BASE64URL.decodeToString(base64UrlEncodedHeader);
            Map<String, Object> m = readValue(origValue);

            if (base64UrlEncodedDigest != null) {
                header = new DefaultJwsHeader(m);
            } else {
                header = new DefaultHeader(m);
            }
        }

        // =============== Body =================
        String payload = TextCodec.BASE64URL.decodeToString(base64UrlEncodedPayload);

        Claims claims = null;

        if (payload.charAt(0) == '{' && payload.charAt(payload.length() - 1) == '}') { //likely to be json, parse it:
            Map<String, Object> claimsMap = readValue(payload);
            claims = new DefaultClaims(claimsMap);
        }

        Object body = claims != null ? claims : payload;

        if (base64UrlEncodedDigest != null) {
            return new DefaultJws<>((JwsHeader) header, body, base64UrlEncodedDigest);
        } else {
            return new DefaultJwt<>(header, body);
        }
    }

    public <T> T parse(String compact, JwtHandler<T> handler)
        throws ExpiredJwtException, MalformedJwtException, SignatureException {
        Assert.notNull(handler, "JwtHandler argument cannot be null.");
        Assert.hasText(compact, "JWT String argument cannot be null or empty.");

        Jwt jwt = parse(compact);

        if (jwt instanceof Jws) {
            Jws jws = (Jws) jwt;
            Object body = jws.getBody();
            if (body instanceof Claims) {
                return handler.onClaimsJws((Jws<Claims>) jws);
            } else {
                return handler.onPlaintextJws((Jws<String>) jws);
            }
        } else {
            Object body = jwt.getBody();
            if (body instanceof Claims) {
                return handler.onClaimsJwt((Jwt<Header, Claims>) jwt);
            } else {
                return handler.onPlaintextJwt((Jwt<Header, String>) jwt);
            }
        }
    }

    public Jwt<Header, String> parsePlaintextJwt(String plaintextJwt) {
        return parse(plaintextJwt, new JwtHandlerAdapter<Jwt<Header, String>>() {
            @Override
            public Jwt<Header, String> onPlaintextJwt(Jwt<Header, String> jwt) {
                return jwt;
            }
        });
    }

    public Jwt<Header, Claims> parseClaimsJwt(String claimsJwt) {
        try {
            return parse(claimsJwt, new JwtHandlerAdapter<Jwt<Header, Claims>>() {
                @Override
                public Jwt<Header, Claims> onClaimsJwt(Jwt<Header, Claims> jwt) {
                    return jwt;
                }
            });
        } catch (IllegalArgumentException iae) {
            throw new UnsupportedJwtException("Signed JWSs are not supported.", iae);
        }
    }

    public Jws<String> parsePlaintextJws(String plaintextJws) {
        try {
            return parse(plaintextJws, new JwtHandlerAdapter<Jws<String>>() {
                @Override
                public Jws<String> onPlaintextJws(Jws<String> jws) {
                    return jws;
                }
            });
        } catch (IllegalArgumentException iae) {
            throw new UnsupportedJwtException("Signed JWSs are not supported.", iae);
        }
    }

    public Jws<Claims> parseClaimsJws(String claimsJws) {
        return parse(claimsJws, new JwtHandlerAdapter<Jws<Claims>>() {
            @Override
            public Jws<Claims> onClaimsJws(Jws<Claims> jws) {
                return jws;
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> readValue(String val) {
        try {
            return objectMapper.readValue(val, Map.class);
        } catch (IOException e) {
            throw new MalformedJwtException("Unable to read JSON value: " + val, e);
        }
    }
}
