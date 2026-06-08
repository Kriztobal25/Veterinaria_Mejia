package com.Veterinaria.Mejia.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Veterinaria.Mejia.services.UsuarioService;

@Controller
public class AuthController {
    
    // Declaramos el servicio como final para garantizar la inmutabilidad
    private final UsuarioService usuarioService;

    // Inyección por constructor: Es la mejor práctica recomendada por Spring (ya no requiere @Autowired aquí)
    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // ==========================================
    // 1. RUTAS DE INICIO DE SESIÓN
    // ==========================================

    @GetMapping("/")
    public String redireccionarALogin() {
        return "redirect:/login";
    }
    
    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String error, 
                            @RequestParam(required = false) String success, 
                            Model model) {
        
        // Mensaje de error (Viene automático de Spring Security si falla)
        if (error != null) {
            model.addAttribute("errorMsg", "Credenciales inválidas o el usuario se encuentra inactivo.");
        }
        
        // Mensaje de éxito (Viene de nuestra recuperación de contraseña)
        if (success != null) {
            model.addAttribute("successMsg", "¡Contraseña actualizada con éxito! Por favor, inicia sesión.");
        }
        
        return "auth/login";
    }

    // ==========================================
    // 2. RUTAS DE RECUPERACIÓN DE CONTRASEÑA
    // ==========================================

    // Muestra el formulario HTML para recuperar la clave
    @GetMapping("/recuperar-password")
    public String mostrarFormularioRecuperacion() {
        return "auth/recuperar-password";
    }

    // Atrapa los datos del formulario cuando el usuario le da a "Guardar nueva clave"
    @PostMapping("/recuperar-password")
    public String procesarRecuperacion(@RequestParam("nombreUsuario") String nombreUsuario,
                                       @RequestParam("respuestaSecreta") String respuestaSecreta,
                                       @RequestParam("nuevaContrasena") String nuevaContrasena,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        try {
            // Llamamos a nuestro motor lógico
            usuarioService.recuperarContrasenaPorPreguntaSecreta(nombreUsuario, respuestaSecreta, nuevaContrasena);
            
            // Si no hay errores, redirigimos al login enviando la bandera de éxito por URL (?success=true)
            redirectAttributes.addAttribute("success", "true");
            return "redirect:/login";
            
        } catch (RuntimeException e) { 
            // Si la respuesta es incorrecta o la clave no cumple las reglas, devolvemos el error a la vista
            model.addAttribute("errorMsg", e.getMessage());
            
            // Devolvemos el usuario ingresado para evitar que lo digite de nuevo
            model.addAttribute("nombreUsuarioDigitado", nombreUsuario); 
            
            return "auth/recuperar-password";
        }
    }
}