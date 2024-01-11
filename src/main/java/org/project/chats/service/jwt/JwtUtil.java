package org.project.chats.service.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.chats.config.jwt.JwtProperties;
import org.project.chats.dto.GeneratedTokenDto;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtUtil implements InitializingBean {

    private final JwtProperties jwtProperties;
//    private final RefreshTokenService tokenService;

    private Key signingKey;

    // signingKey 생성

    @Override
    public void afterPropertiesSet() throws Exception {
        byte[] secretKeyByte = jwtProperties.getSecretKey().getBytes();
        signingKey = Keys.hmacShaKeyFor(secretKeyByte);
    }

    public GeneratedTokenDto generatedToken(String email, String role) {
        // refreshToken과 accessToken을 생성한다.
        String refreshToken = generateRefreshToken(email, role);
        String accessToken = generateAccessToken(email, role);

        // 토큰을 Redis에 저장한다.
//        tokenService.saveTokenInfo(email, refreshToken, accessToken);
        return new GeneratedTokenDto(accessToken, refreshToken);
    }

    public String generateRefreshToken(String email, String role) {
        // 토큰 유효기간을 밀리초 단위로 설정
        long refreshPeriod = 1000L * 60L * 60L * 24L * 14; // 2주

        // 새로운 클레임 객체를 생성하고, 이메일과 역할(권한)을 셋팅
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);

        // 현재 시간과 날짜를 가져온다
        Date now = new Date();

        return Jwts.builder()
                // Payload를 구성하는 속성들을 정의한다.
                .setClaims(claims)
                // 발행일자를 넣는다.
                .setIssuedAt(now)
                // 토큰의 만료일시를 설정한다.
                .setExpiration(new Date(now.getTime() + refreshPeriod))
                // 지정된 서명 알고리즘과 비밀 키를 사용하여 토큰을 서명한다.
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAccessToken(String email, String role) {
        long tokenPeriod = 1000L * 60L * 30L; // 30분
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("role", role);

        Date now = new Date();
        return
                Jwts.builder()
                        // Payload를 구성하는 속성들을 정의한다.
                        .setClaims(claims)
                        // 발행일자를 넣는다.
                        .setIssuedAt(now)
                        // 토큰의 만료일시를 설정한다.
                        .setExpiration(new Date(now.getTime() + tokenPeriod))
                        // 지정된 서명 알고리즘과 비밀 키를 사용하여 토큰을 서명한다.
                        .signWith(signingKey, SignatureAlgorithm.HS256)
                        .compact();

    }

    public boolean verifyToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)  // 비밀키를 설정하여 파싱한다.
                    .build().parseClaimsJws(token); // 주어진 토큰을 파싱하여 Claims 객체를 얻는다.
            // 토큰의 만료 시간과 현재 시간 비교
            return claims.getBody()
                    .getExpiration()
                    .after(new Date()); // 만료 시간이 현재 시간 이후인지 확인하여 유효성 검사 결과를 반환
        } catch (Exception e) {
            return false;
        }
    }

    // 토큰에서 Email을 추출한다.
    public String getEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰에서 ROLE(권한)만 추출한다.
    public String getRole(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
}
