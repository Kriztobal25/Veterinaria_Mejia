package com.Veterinaria.Mejia.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Veterinaria.Mejia.models.Producto;
import com.Veterinaria.Mejia.repository.ProductoRepository;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    // Quitamos el IngresoStockRepository porque esa lógica ya vive en su propio servicio

    public List<Producto> listarTodos() {
        return productoRepository.findAll();
    }

    // NUEVO: Buscador inteligente para el inventario (Filtro por nombre, categoría y especie)
    public List<Producto> buscarInventario(String nombre, Integer categoriaId, Integer especieId) {
        return productoRepository.buscarYFiltrarInventarioJPQL(nombre, categoriaId, especieId);
    }

    public List<Producto> listarStockCritico() {
        // En tu repositorio debe retornar los que tienen stock_total <= 12.00
        return productoRepository.buscarProductosStockCriticoJPQL();
    }

    // REGISTRO DE NUEVO PRODUCTO
    @Transactional
    public Producto guardarProductoNuevo(Producto producto) {
        // Aseguramos que las reglas de negocio base se cumplan desde el backend
        producto.setEstado(true); // Siempre activo al crearse
        
        if (producto.getStockTotal().compareTo(new BigDecimal("99.00")) > 0) {
            throw new IllegalArgumentException("Regla de negocio: El stock inicial no puede exceder las 99 unidades/kg/litros.");
        }
        
        return productoRepository.save(producto);
    }

    // EL MÉTODO registrarIngresoMercaderia FUE ELIMINADO PORQUE ESA LÓGICA AHORA PERTENECE A IngresoStockService

    // CAMBIO DE ESTADO (Activo / Inactivo) - Solo gestionado en Inventario
    @Transactional
    public void modificarEstado(Integer idProducto, boolean nuevoEstado) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado."));
        
        // REGLA DE NEGOCIO ACTUALIZADA: Bloquear inactivación si hay stock físico
        if (!nuevoEstado && producto.getStockTotal().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Bloqueo de seguridad: No puedes inactivar un producto que aún tiene stock físico (" + producto.getStockTotal() + " " + producto.getTipoUnidad() + "). Debes desecharlo o venderlo primero.");
        }
        
        producto.setEstado(nuevoEstado);
        productoRepository.save(producto);
    }

    // GESTIÓN DE PÉRDIDAS Y DESECHOS (Mermas)
    @Transactional
    public void reportarMermaDesecho(Integer idProducto, BigDecimal cantidadDesechada) {
        Producto producto = productoRepository.findById(idProducto)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado."));

        if (producto.getStockTotal().compareTo(cantidadDesechada) < 0) {
            throw new IllegalArgumentException("No hay suficiente stock en sistema para desechar esta cantidad.");
        }

        // Restamos del inventario la pérdida real (saco roto, producto vencido, etc.)
        producto.setStockTotal(producto.getStockTotal().subtract(cantidadDesechada));
        productoRepository.save(producto);

        // Dinero perdido
        BigDecimal perdidaEconomica = cantidadDesechada.multiply(producto.getPrecioInversion());
        System.out.println("ALERTA MERMA: Se registró una pérdida económica de S/ " + perdidaEconomica 
                           + " por desecho del producto " + producto.getNombre());
        
        // *Nota: En un futuro puedes guardar la variable 'perdidaEconomica' en una tabla "Mermas" 
        // para cruzarlo con el reporte de ganancias.
    }
}