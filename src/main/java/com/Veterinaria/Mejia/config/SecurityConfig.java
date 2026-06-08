package com.Veterinaria.Mejia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Implementación de BCrypt requerida
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Desactivado para peticiones locales POST
            
            // 1. CONTROL ESTRICTO DE ACCESOS POR AUTORIDAD EXACTA
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas y recursos estáticos
                .requestMatchers("/login", "/recuperar-password", "/css/**", "/js/**", "/images/**").permitAll()
                
                // Módulos exclusivos del Dueño / Administrador
                .requestMatchers(
                    "/usuarios/**", 
                    "/almacen/proveedores/**", 
                    "/almacen/ingresos/**", 
                    "/mantenimiento/categorias/**",
                    "/reportes/**"
                ).hasAuthority("ROLE_Administrador")
                
                // Módulos operativos compartidos (Ventas, Catálogos, Tarifario, API y el Dashboard principal)
                .requestMatchers(
                    "/ventas/**", 
                    "/almacen/productos/**", 
                    "/mantenimiento/servicios/**", 
                    "/api/utilidades/**",
                    "/dashboard"
                ).hasAnyAuthority("ROLE_Administrador", "ROLE_Empleado")
                
                // Cualquier otra ruta no especificada arriba requiere estar autenticado
                .anyRequest().authenticated()
            )
            
            // 2. FILTRO AUTOMÁTICO INTEGRADO (Spring Security maneja el login)
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")   // Intercepta el POST de forma nativa
                .usernameParameter("usuario")    // Vincula con el input name="usuario" en auth/login.html
                .passwordParameter("pass")       // Vincula con el input name="pass" en auth/login.html
                .defaultSuccessUrl("/dashboard", true) // Redirección directa al Index/Dashboard al tener éxito
                .failureUrl("/login?error=true")       // Redirección si la contraseña/usuario es incorrecto
                .permitAll()
            )
            
            // 3. CIERRE DE SESIÓN SEGURO
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }
}