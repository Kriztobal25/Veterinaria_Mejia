package com.Veterinaria.Mejia.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Veterinaria.Mejia.models.DetalleIngresoStock;
import com.Veterinaria.Mejia.models.IngresoStock;
import com.Veterinaria.Mejia.models.Producto;
import com.Veterinaria.Mejia.repository.IngresoStockRepository;
import com.Veterinaria.Mejia.repository.ProductoRepository;

@Service
public class IngresoStockService {

    @Autowired
    private IngresoStockRepository ingresoStockRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // ==========================================
    // 1. LISTAR HISTORIAL DE INGRESOS
    // ==========================================
    public List<IngresoStock> listarTodos() {
        // Retorna todas las cabeceras de compras a proveedores
        return ingresoStockRepository.findAll();
    }

    // ==========================================
    // 2. REGISTRAR INGRESO Y SUMAR STOCK
    // ==========================================
    @Transactional
    public IngresoStock registrarIngresoMercaderia(IngresoStock ingresoStock) {
        
        // 1. Validar que la cabecera tenga fecha, si no, le ponemos la de hoy
        if (ingresoStock.getFechaIngreso() == null) {
            ingresoStock.setFechaIngreso(LocalDateTime.now());
        }

        // 2. Procesar la lista de productos (El carrito)
        if (ingresoStock.getDetallesIngreso() != null && !ingresoStock.getDetallesIngreso().isEmpty()) {
            
            for (DetalleIngresoStock detalle : ingresoStock.getDetallesIngreso()) {
                
                // Regla de Negocio: Máximo 99 unidades/kg por lote
                if (detalle.getCantidad().compareTo(new BigDecimal("99.00")) > 0) {
                    throw new IllegalArgumentException("Bloqueo de seguridad: No puedes ingresar un lote mayor a 99 unidades de golpe para un mismo producto.");
                }

                // Buscamos el producto físico en la base de datos
                Producto producto = productoRepository.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("El producto seleccionado no existe en el catálogo."));

                // Sumamos la cantidad ingresada al stock total que ya tenía el producto
                producto.setStockTotal(producto.getStockTotal().add(detalle.getCantidad()));
                
                // Guardamos el producto con su nuevo stock
                productoRepository.save(producto);
                
                // Amarramos el detalle a la cabecera para que MySQL entienda la relación (Llave Foránea)
                detalle.setIngresoStock(ingresoStock);
            }
        } else {
            throw new IllegalArgumentException("No puedes registrar un ingreso vacío. Agrega al menos un producto.");
        }

        // 3. Finalmente guardamos la cabecera (Hibernate guardará los detalles en cascada automáticamente)
        return ingresoStockRepository.save(ingresoStock);
    }
}