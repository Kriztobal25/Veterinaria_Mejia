package com.Veterinaria.Mejia.controllers;

import java.util.List;

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

import com.Veterinaria.Mejia.models.Categoria;
import com.Veterinaria.Mejia.models.Especie;
import com.Veterinaria.Mejia.models.Producto;
import com.Veterinaria.Mejia.repository.CategoriaRepository;
import com.Veterinaria.Mejia.repository.EspecieRepository;
import com.Veterinaria.Mejia.repository.ProductoRepository;
import com.Veterinaria.Mejia.services.CategoriaService;
import com.Veterinaria.Mejia.services.ProductoService;

@Controller
@RequestMapping("/almacen/productos") // 🚨 Cambiamos la ruta base para que sea más específica
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EspecieRepository especieRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    // Eliminamos ProveedorService, ServicioService y todos los métodos de IngresoStock.
    // ¡Esos ya tienen sus propios controladores!

    // ==========================================
    // 1. CATÁLOGO DE PRODUCTOS (Con Filtros)
    // ==========================================
    @GetMapping
    public String listarProductosFiltrados(
            @RequestParam(value = "categoriaId", required = false) Integer categoriaId,
            @RequestParam(value = "especieId", required = false) Integer especieId,
            Model model) {
        
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("especies", especieRepository.findAll());
        
        model.addAttribute("categoriaSeleccionadaId", categoriaId);
        model.addAttribute("especieSeleccionadaId", especieId);

        // Si el usuario aplicó filtros, buscamos en JPQL, si no, traemos todos
        if (categoriaId != null && especieId != null) {
            List<Producto> filtrados = productoRepository.buscarPorCategoriaYEspecieJPQL(categoriaId, especieId);
            model.addAttribute("productos", filtrados);
        } else {
            model.addAttribute("productos", productoService.listarTodos());
        }

        return "almacen/lista-productos";
    }

    // ==========================================
    // 2. FORMULARIO PARA CREAR PRODUCTO
    // ==========================================
    @GetMapping("/nuevo")
    public String mostrarFormularioProducto(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("categorias", categoriaService.listarTodas());
        model.addAttribute("especies", especieRepository.findAll());
        
        return "almacen/form-producto"; // Apunta a tu nuevo HTML independiente
    }

    // ==========================================
    // 3. GUARDAR PRODUCTO EN EL CATÁLOGO
    // ==========================================
    @PostMapping("/guardar")
    public String guardarNuevoProducto(@ModelAttribute Producto producto, RedirectAttributes redirectAttrs) {
        
        // Validación manual de llaves foráneas para evitar errores de Hibernate
        if (producto.getEspecie() != null && producto.getEspecie().getId() != null && producto.getEspecie().getId() > 0) {
            Especie especieReal = especieRepository.findById(producto.getEspecie().getId()).orElse(null);
            producto.setEspecie(especieReal);
        } else {
            producto.setEspecie(null); 
        }

        if (producto.getCategoria() != null && producto.getCategoria().getId() != null && producto.getCategoria().getId() > 0) {
            Categoria categoriaReal = categoriaRepository.findById(producto.getCategoria().getId()).orElse(null);
            producto.setCategoria(categoriaReal);
        } else {
            producto.setCategoria(null);
        }

        // Reglas de negocio iniciales
        producto.setEstado(true); 
        producto.setStockTotal(java.math.BigDecimal.ZERO); // Todo producto nace con stock 0 hasta que se le haga un IngresoStock
        
        try {
            productoService.guardarProductoNuevo(producto);
            redirectAttrs.addFlashAttribute("successMsg", "El producto '" + producto.getNombre() + "' ha sido catalogado con éxito.");
        } catch (Exception e) {
            e.printStackTrace(); 
            redirectAttrs.addFlashAttribute("errorMsg", "Error al guardar: Asegúrese de seleccionar una Categoría válida.");
            return "redirect:/almacen/productos/nuevo";
        }
        
        // Lo devolvemos al catálogo general tras crearlo
        return "redirect:/almacen/productos";
    }

    // ==========================================
    // 4. CAMBIAR ESTADO (Activar / Inactivar)
    // ==========================================
    @GetMapping("/cambiar-estado/{id}")
    public String cambiarEstadoProducto(@PathVariable("id") Integer id, 
                                        @RequestParam(value = "catId", required = false) Integer catId,
                                        @RequestParam(value = "espId", required = false) Integer espId,
                                        RedirectAttributes redirectAttrs) {
        try {
            Producto prod = productoRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("El producto solicitado no existe."));
            
            // Usamos tu servicio que ya tiene la regla: "No inactivar si hay stock"
            productoService.modificarEstado(id, !prod.getEstado());
            redirectAttrs.addFlashAttribute("successMsg", "Estado del producto actualizado.");
            
        } catch (RuntimeException e) { // 🚨 CORRECCIÓN: Dejamos solo al "padre" RuntimeException
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
        }
        
        // Reconstruimos la URL de retorno manteniendo los filtros si existían
        String urlRetorno = "redirect:/almacen/productos";
        if (catId != null && espId != null) {
            urlRetorno += "?categoriaId=" + catId + "&especieId=" + espId;
        }
        
        return urlRetorno;
    }
}