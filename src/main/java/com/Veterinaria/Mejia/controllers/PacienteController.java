package com.Veterinaria.Mejia.controllers;

import com.Veterinaria.Mejia.models.Cliente;
import com.Veterinaria.Mejia.models.Especie;
import com.Veterinaria.Mejia.models.Paciente;
import com.Veterinaria.Mejia.repository.ClienteRepository;
import com.Veterinaria.Mejia.repository.EspecieRepository;
import com.Veterinaria.Mejia.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteRepository pacienteRepo;

    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private EspecieRepository especieRepo;

    // Inyectar la ruta desde application.properties
    @Value("${app.upload.pacientes-dir}")
    private String carpetaFotos;

    // Extensiones permitidas para fotos
    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp");

    @GetMapping
    public String listarPacientes(Model model) {
        model.addAttribute("pacientes", pacienteRepo.findByEstadoTrue());
        return "pacientes/lista-pacientes";
    }

    @GetMapping("/nuevo")
    public String formNuevoPaciente(Model model) {
        model.addAttribute("paciente", new Paciente());
        model.addAttribute("clientes", clienteRepo.findAll());
        model.addAttribute("especies", especieRepo.findAll());
        return "pacientes/form-paciente";
    }

    @PostMapping("/guardar")
    public String guardarPaciente(
            @ModelAttribute Paciente paciente,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            RedirectAttributes ra) {

        // VALIDACIÓN 1: Verificar que el cliente exista
        if (paciente.getCliente() == null || paciente.getCliente().getId() == null) {
            ra.addFlashAttribute("errorMsg", "Debe seleccionar un tutor/cliente para el paciente.");
            return "redirect:/pacientes/nuevo";
        }

        Cliente clienteExiste = clienteRepo.findById(paciente.getCliente().getId()).orElse(null);
        if (clienteExiste == null) {
            ra.addFlashAttribute("errorMsg", "El tutor seleccionado no existe en el sistema.");
            return "redirect:/pacientes/nuevo";
        }
        paciente.setCliente(clienteExiste);

        // VALIDACIÓN 2: Verificar que la especie exista
        if (paciente.getEspecie() == null || paciente.getEspecie().getId() == null) {
            ra.addFlashAttribute("errorMsg", "Debe seleccionar una especie para el paciente.");
            return "redirect:/pacientes/nuevo";
        }

        Especie especieExiste = especieRepo.findById(paciente.getEspecie().getId()).orElse(null);
        if (especieExiste == null) {
            ra.addFlashAttribute("errorMsg", "La especie seleccionada no existe en el sistema.");
            return "redirect:/pacientes/nuevo";
        }
        paciente.setEspecie(especieExiste);

        try {
            // Generar número de historia automático si viene vacío
            if (paciente.getNumeroHistoria() == null || paciente.getNumeroHistoria().isBlank()) {
                paciente.setNumeroHistoria("HC-" + System.currentTimeMillis());
            }

            // VALIDACIÓN 3: Procesar la foto SOLO si se subió una
            if (foto != null && !foto.isEmpty()) {
                // Validar tamaño (máximo 5MB)
                if (foto.getSize() > 5 * 1024 * 1024) {
                    ra.addFlashAttribute("errorMsg", "La foto no debe superar los 5MB.");
                    return "redirect:/pacientes/nuevo";
                }

                // Validar extensión
                String nombreOriginal = foto.getOriginalFilename();
                if (nombreOriginal == null || !nombreOriginal.contains(".")) {
                    ra.addFlashAttribute("errorMsg", "El archivo de foto no tiene una extensión válida.");
                    return "redirect:/pacientes/nuevo";
                }

                String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf(".")).toLowerCase();
                if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
                    ra.addFlashAttribute("errorMsg", "Solo se permiten imágenes en formato JPG, PNG, GIF o WEBP.");
                    return "redirect:/pacientes/nuevo";
                }

                // Generar nombre único
                String nombreUnico = UUID.randomUUID().toString() + extension;

                // Crear directorio si no existe
                Path directorio = Paths.get(carpetaFotos);
                if (!Files.exists(directorio)) {
                    Files.createDirectories(directorio);
                }

                // Guardar archivo
                Path rutaCompleta = directorio.resolve(nombreUnico);
                Files.copy(foto.getInputStream(), rutaCompleta, StandardCopyOption.REPLACE_EXISTING);

                // Guardar ruta relativa para mostrar en el HTML
                paciente.setRutaFoto("/pacientes/" + nombreUnico);
            }

            pacienteRepo.save(paciente);
            ra.addFlashAttribute("successMsg", "Paciente '" + paciente.getNombre() + "' registrado exitosamente.");

        } catch (IOException e) {
            ra.addFlashAttribute("errorMsg", "Error al guardar la foto: " + e.getMessage());
            return "redirect:/pacientes/nuevo";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMsg", "Error inesperado al guardar el paciente: " + e.getMessage());
            return "redirect:/pacientes/nuevo";
        }

        return "redirect:/pacientes";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarPaciente(@PathVariable Integer id, RedirectAttributes ra) {
        Paciente p = pacienteRepo.findById(id).orElse(null);
        if (p != null) {
            // Borrado lógico (no eliminar de la BD)
            p.setEstado(false);
            pacienteRepo.save(p);
            ra.addFlashAttribute("successMsg", "Paciente '" + p.getNombre() + "' dado de baja correctamente.");
        } else {
            ra.addFlashAttribute("errorMsg", "No se encontró el paciente especificado.");
        }
        return "redirect:/pacientes";
    }

    @GetMapping("/editar/{id}")
    public String formEditarPaciente(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Paciente paciente = pacienteRepo.findById(id).orElse(null);
        if (paciente == null) {
            ra.addFlashAttribute("errorMsg", "No se encontró el paciente especificado.");
            return "redirect:/pacientes";
        }
        model.addAttribute("paciente", paciente);
        model.addAttribute("clientes", clienteRepo.findAll());
        model.addAttribute("especies", especieRepo.findAll());
        return "pacientes/form-paciente";
    }
}