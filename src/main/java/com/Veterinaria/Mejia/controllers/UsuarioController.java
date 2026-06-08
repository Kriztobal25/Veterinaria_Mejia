package com.Veterinaria.Mejia.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.Veterinaria.Mejia.models.Role;
import com.Veterinaria.Mejia.models.Usuario;
import com.Veterinaria.Mejia.repository.RoleRepository;
import com.Veterinaria.Mejia.services.UsuarioService;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Inyectamos el repositorio de roles para poder llenar el <select> en el formulario
    @Autowired
    private RoleRepository roleRepository; 

    // ==========================================
    // 1. PANEL DE GESTIÓN DE USUARIOS
    // ==========================================
    @GetMapping
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("roles", roleRepository.findAll()); // Para los modales de cambio de rol
        return "usuarios/gestion-usuarios";
    }

    // ==========================================
    // 2. FORMULARIO DE NUEVO USUARIO
    // ==========================================
    @GetMapping("/nuevo")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", roleRepository.findAll());
        return "usuarios/form-usuario"; // Asumiendo que creaste un HTML para este formulario
    }

    // ==========================================
    // 3. GUARDAR USUARIO (Atrapando tus reglas de seguridad)
    // ==========================================
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, RedirectAttributes redirectAttrs) {
        try {
            usuarioService.guardarUsuarioNuevo(usuario);
            redirectAttrs.addFlashAttribute("successMsg", "El usuario '" + usuario.getNombreUsuario() + "' ha sido registrado con éxito.");
            return "redirect:/usuarios";
            
        } catch (RuntimeException e) {
            // Atrapa la validación de los 5 usuarios, la contraseña débil o el nombre duplicado
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            
            // Lo devolvemos al formulario para que intente de nuevo
            return "redirect:/usuarios/nuevo";
        }
    }

    // ==========================================
    // 4. BLOQUEAR / DESBLOQUEAR ACCESO
    // ==========================================
    @GetMapping("/cambiar-estado/{id}")
    public String cambiarEstadoUsuario(@PathVariable("id") Integer id, RedirectAttributes redirectAttrs) {
        try {
            Usuario usuario = usuarioService.buscarPorId(id);
            
            // Invertimos el estado actual (Si era true, pasa a false)
            usuarioService.modificarEstado(id, !usuario.getEstado());
            
            String accion = usuario.getEstado() ? "desactivado" : "activado"; // Mensaje dinámico
            redirectAttrs.addFlashAttribute("successMsg", "El usuario ha sido " + accion + " correctamente.");
            
        } catch (RuntimeException e) {
            // Atrapa tu regla de "No puedes desactivar al único administrador"
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        
        return "redirect:/usuarios";
    }

    // ==========================================
    // 5. CAMBIAR ROL (Desde un select/modal en la lista)
    // ==========================================
    @PostMapping("/cambiar-rol")
    public String cambiarRolUsuario(@RequestParam("idUsuario") Integer idUsuario, 
                                    @RequestParam("idRol") Integer idRol, 
                                    RedirectAttributes redirectAttrs) {
        try {
            Role nuevoRol = roleRepository.findById(idRol)
                    .orElseThrow(() -> new RuntimeException("El rol seleccionado no existe."));
            
            usuarioService.cambiarRol(idUsuario, nuevoRol);
            redirectAttrs.addFlashAttribute("successMsg", "Los permisos del usuario han sido actualizados.");
            
        } catch (RuntimeException e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        
        return "redirect:/usuarios";
    }
}