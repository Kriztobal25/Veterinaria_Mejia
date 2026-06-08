package com.Veterinaria.Mejia.controllers;

import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.Veterinaria.Mejia.models.Venta;
import com.Veterinaria.Mejia.services.CategoriaService;
import com.Veterinaria.Mejia.services.ClienteService;
import com.Veterinaria.Mejia.services.EspecieService;
import com.Veterinaria.Mejia.services.ProductoService;
import com.Veterinaria.Mejia.services.ServicioService;
import com.Veterinaria.Mejia.services.VentaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final ServicioService servicioService;
    private final CategoriaService categoriaService;
    private final EspecieService especieService;
    private final ClienteService clienteService; 

    // ==========================================
    // 1. NUEVO MÉTODO: HISTORIAL DE VENTAS (Soluciona el 404)
    // ==========================================
    @GetMapping
    public String historialVentas(Model model) {
        // Asumo que tienes un método listarTodas() o findAll() en tu VentaService. 
        // Si se llama distinto, solo ajusta el nombre del método aquí abajo:
        model.addAttribute("ventas", ventaService.listarTodas()); 
        
        return "ventas/historial-ventas";
    }

    // ==========================================
    // 2. PANEL DE CAJA (NUEVA VENTA)
    // ==========================================
    @GetMapping("/nuevo")
    public String nuevaVenta(Model model) {
        Venta nuevaVenta = new Venta();
        
        // Hardcodeo temporal del ID de usuario encargado de la caja
        nuevaVenta.setUsuarioId(1); 
        
        // Inicializa la lista vacía para el carrito
        nuevaVenta.setDetallesVentas(new ArrayList<>());

        model.addAttribute("venta", nuevaVenta);
        
        // Carga de elementos comunes de la interfaz
        cargarElementosInterfaz(model);
        
        return "ventas/panel-caja";
    }

    // ==========================================
    // 3. PROCESAMIENTO DE TRANSACCIÓN
    // ==========================================
    @PostMapping("/procesar")
    public String procesarVenta(@Valid @ModelAttribute("venta") Venta venta, 
                                // 🚨 CORRECCIÓN: Atrapamos los inputs sueltos del DNI y Nombre desde el HTML
                                @RequestParam(value = "dniIngresado", required = false) String dniIngresado,
                                @RequestParam(value = "nombreIngresado", required = false) String nombreIngresado,
                                BindingResult result, Model model) {
        if (result.hasErrors()) {
            cargarElementosInterfaz(model);
            return "ventas/panel-caja";
        }

        try {
            // Aseguramos que la venta nazca como "Activa" (true) para que funcione tu método de anulación
            venta.setEstado(true); 

            // 🚨 CORRECCIÓN: Enviamos los 3 parámetros exactos que pide tu VentaService
            Venta ventaProcesada = ventaService.procesarVentaTransaccional(venta, dniIngresado, nombreIngresado);
            
            return "redirect:/ventas/imprimir/" + ventaProcesada.getId();
            
        } catch (RuntimeException e) {
            model.addAttribute("errorStock", e.getMessage());
            cargarElementosInterfaz(model);
            return "ventas/panel-caja";
        }
    }

    // ==========================================
    // 4. GENERACIÓN DE COMPROBANTE
    // ==========================================
    @GetMapping("/imprimir/{id}")
    public String mostrarTicketImpresion(@PathVariable("id") Integer id, Model model) {
        Venta ventaReal = ventaService.buscarPorId(id);
        model.addAttribute("venta", ventaReal);
        return "ventas/comprobante-ticket";
    }

    /**
     * Alimenta el modelo con los catálogos requeridos por la vista 'panel-caja'.
     */
    private void cargarElementosInterfaz(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        model.addAttribute("servicios", servicioService.listarActivosPOS());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("especies", especieService.listarTodas());
        model.addAttribute("clientes", clienteService.findAll()); 
    }
}