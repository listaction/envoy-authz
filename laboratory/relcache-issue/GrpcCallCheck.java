package com.company;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import io.envoyproxy.envoy.service.auth.v3.AttributeContext;
import io.envoyproxy.envoy.service.auth.v3.AuthorizationGrpc;
import io.envoyproxy.envoy.service.auth.v3.CheckRequest;
import io.envoyproxy.envoy.service.auth.v3.CheckResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

//add libs
//auth0.java.jwt
//io.envoyproxy.controlplane.api

public class GrpcCallCheck {
    private static final String USER = "23a98b18-430b-4582-aba5-93c245b48661"; // from DB - authz.rel_cache
    private static final String ISSUER_ID = "/realms/venues-crg";
    private static final String REQUEST_METHOD = "GET";
    private static final String PATH = "/account/user/profile"; // from DB - authz.rel_cache

    private static final int DEFAULT_PORT = 8180; // first instance - 8180, second - 8181, etc
    private static final int INSTANCE_COUNT = 10; // ports will be from 8180 to 8189

    private static final int REQUESTS_COUNT = 1000;

    public static void main(String[] args) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "postgres");
        removeCache(connection);

        // There will be runned INSTANCE_COUNT of threads, each thread will sent REQUESTS_COUNT of request
        IntStream.range(0, INSTANCE_COUNT)
                .parallel()
                .forEach((index) ->
                        IntStream.range(0, REQUESTS_COUNT)
                        .forEach((ignored) -> sendRequest(index, connection))
                );
    }

    private static void sendRequest(int index, Connection connection) {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("authorization", "Bearer " + generateToken(USER, ISSUER_ID));

        AttributeContext.HttpRequest httpRequest = AttributeContext.HttpRequest.newBuilder()
                .setMethod(REQUEST_METHOD)
                .setPath(PATH)
                .putAllHeaders(headersMap)
                .build();

        AttributeContext.Request request = AttributeContext.Request.newBuilder()
                .setHttp(httpRequest)
                .build();

        AttributeContext attributeContext = AttributeContext.newBuilder()
                .setRequest(request)
                .build();

        CheckRequest checkRequest = CheckRequest.newBuilder()
                .setAttributes(attributeContext)
                .build();

        int increment = index % INSTANCE_COUNT;
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", DEFAULT_PORT + increment).usePlaintext().build();

        AuthorizationGrpc.AuthorizationBlockingStub stub = AuthorizationGrpc.newBlockingStub(channel);

        CheckResponse response = stub.check(checkRequest);

        channel.shutdown();

        removeCache(connection);
    }

    private static String generateToken(String user, String issuerId) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomFgp = new byte[50];
        secureRandom.nextBytes(randomFgp);
        String userFingerprint = DatatypeConverter.printHexBinary(randomFgp);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] userFingerprintDigest = Objects.requireNonNull(digest).digest(userFingerprint.getBytes(StandardCharsets.UTF_8));
        String userFingerprintHash = DatatypeConverter.printHexBinary(userFingerprintDigest);

        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        c.add(Calendar.HOUR, 15);
        Date expirationDate = c.getTime();
        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("typ", "JWT");
        return JWT.create().withSubject(user)
                .withExpiresAt(expirationDate)
                .withIssuer(issuerId)
                .withIssuedAt(now)
                .withNotBefore(now)
                .withClaim("userFingerprint", userFingerprintHash)
                .withHeader(headerClaims)
                .sign(Algorithm.HMAC256("secret"));
    }

    private static void removeCache(Connection connection) {
        String sql = String.format("delete from authz.rel_cache rc where rc.usr = '%s'", USER);
        try(Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ignored) {}
    }
}