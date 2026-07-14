package security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.io.IOException;

/**
 * Chỉ xác thực JWT và đặt Authentication vào SecurityContext. Phân quyền URL
 * thuộc về AuthorizationFilter qua cấu hình trong {@link SecurityConfig}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final Key signingKey;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            @Value("${security.jwt.secret}") String jwtSecret,
            JwtAuthenticationEntryPoint authenticationEntryPoint) {
        Objects.requireNonNull(jwtSecret, "security.jwt.secret must not be null");
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("security.jwt.secret must contain at least 32 bytes for HS256");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.authenticationEntryPoint = Objects.requireNonNull(
                authenticationEntryPoint, "authenticationEntryPoint must not be null");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveBearerToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                if (username != null && !username.trim().isEmpty()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    extractAuthorities(claims));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException exception) {
                /*
                 * OncePerRequestFilter thường chạy trước ExceptionTranslationFilter.
                 * Vì vậy chỉ ủy quyền việc tạo response cho entry point tập trung,
                 * không tự ghi JSON hoặc thực hiện authorization tại đây.
                 */
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(
                        request,
                        response,
                        new BadCredentialsException("Invalid or expired JWT", exception));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(Claims claims) {
        Object rawRoles = claims.get("roles");
        if (!(rawRoles instanceof Collection<?>)) {
            return Collections.emptyList();
        }

        Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (Object rawRole : (Collection<?>) rawRoles) {
            if (rawRole == null) {
                continue;
            }
            String role = rawRole.toString().trim();
            if (role.isEmpty()) {
                continue;
            }
            String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
            authorities.add(new SimpleGrantedAuthority(authority));
        }
        return authorities;
    }
}
