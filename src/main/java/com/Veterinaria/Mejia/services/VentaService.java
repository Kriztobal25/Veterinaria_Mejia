package com.Veterinaria.Mejia.services;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Veterinaria.Mejia.models.Cliente;
import com.Veterinaria.Mejia.models.DetalleVenta;
import com.Veterinaria.Mejia.models.Producto;
import com.Veterinaria.Mejia.models.Venta;
import com.Veterinaria.Mejia.repository.ClienteRepository;
import com.Veterinaria.Mejia.repository.ProductoRepository;
import com.Veterinaria.Mejia.repository.VentaRepository;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // INYECTAMOS EL REPOSITORIO DE CLIENTES
    @Autowired
    private ClienteRepository clienteRepository; 
   
    @Transactional
    public Venta procesarVentaTransaccional(Venta venta, String dniIngresado, String nombreIngresado) {
        
        // 1. LÓGICA INTELIGENTE DEL CLIENTE 
        if (dniIngresado != null && !dniIngresado.trim().isEmpty()) {
            
            Optional<Cliente> clienteExistente = clienteRepository.findByDniJPQL(dniIngresado); 
            
            if (clienteExistente.isPresent()) {
                // CORRECCIÓN 1: Agregamos .getId()
                venta.setClienteId(clienteExistente.get().getId()); 
                
            } else {
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setDni(dniIngresado);
                nuevoCliente.setNombre(nombreIngresado != null && !nombreIngresado.trim().isEmpty() ? nombreIngresado : "Cliente sin nombre");
                
                Cliente clienteGuardado = clienteRepository.save(nuevoCliente);
                
                // CORRECCIÓN 2: Agregamos .getId()
                venta.setClienteId(clienteGuardado.getId()); 
            }
        } else {
            // Público General (Nulo también es válido para un Integer)
            venta.setClienteId(null); 
        }

        // 2. REGLA DE NEGOCIO: Evaluación automática del monto de S/. 5.00
        if (venta.getTotalVenta().compareTo(new BigDecimal("5.00")) >= 0) {
            venta.setTipoComprobante("Boleta");
            venta.setSerie("B001");
        } else {
            venta.setTipoComprobante("Ticket");
            venta.setSerie("T001");
        }

        // 3. CONTROL AUTÓNOMO DE CORRELATIVOS VIA JPQL
        Integer ultimoCorrelativo = ventaRepository.obtenerMaximoCorrelativoJPQL(venta.getSerie());
        int nuevoCorrelativo = (ultimoCorrelativo == null) ? 1 : ultimoCorrelativo + 1;
        venta.setCorrelativo(nuevoCorrelativo);

        // =====================================================================
        // 4. ACTUALIZACIÓN DE INVENTARIO Y VALIDACIÓN DE SERVICIOS
        // =====================================================================
        // Usamos un Set (conjunto) para llevar el registro de qué servicios ya se cobraron
        Set<Integer> serviciosCobrados = new HashSet<>();

        for (DetalleVenta detalle : venta.getDetallesVentas()) {
            
            // A) VALIDACIÓN DE SERVICIOS (Evita duplicidad en la misma boleta)
            if (detalle.getServicio() != null) {
                // Si el ID del servicio no se puede añadir al Set (porque ya existe), lanza error
                if (!serviciosCobrados.add(detalle.getServicio().getId())) {
                    throw new IllegalArgumentException("Error: No puedes registrar el mismo servicio más de una vez en la misma boleta.");
                }
            }

            // B) VALIDACIÓN Y DESCUENTO DE PRODUCTOS FÍSICOS
            if (detalle.getProducto() != null) {
                Producto prod = productoRepository.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado en el catálogo."));

                // Validación de seguridad física antes de descontar
                if (prod.getStockTotal().compareTo(detalle.getCantidad()) < 0) {
                    throw new RuntimeException("Stock insuficiente para el producto: " + prod.getNombre());
                }

                // Restamos la cantidad exacta vendida (soporta decimales como 0.50 kg)
                prod.setStockTotal(prod.getStockTotal().subtract(detalle.getCantidad()));
                productoRepository.save(prod);
            }
            
            // Vinculamos de forma bidireccional el detalle con su cabecera de venta
            detalle.setVenta(venta);
        }

        // 5. Guardamos de forma definitiva la cabecera y todos sus detalles en cascada en MySQL
        return ventaRepository.save(venta);
    }

    // Busca una venta por ID para alimentar la pantalla de impresión del ticket térmico
    public Venta buscarPorId(Integer id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transacción de venta no encontrada."));
    }

    // =========================================================================
    // LISTADO DE VENTAS
    // =========================================================================
    public java.util.List<Venta> listarTodas() {
        return ventaRepository.findAll();
    }
    
    // =========================================================================
    // ANULACIÓN DE VENTA (EXTORNO) Y DEVOLUCIÓN DE STOCK
    // =========================================================================
    @Transactional
    public void anularVenta(Integer idVenta) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("Transacción de venta no encontrada."));

        // 1. Validar que la venta no esté anulada previamente
        if (!venta.getEstado()) {
            throw new IllegalArgumentException("Esta venta ya se encuentra anulada.");
        }

        // 2. Devolver el stock de los productos al inventario
        for (DetalleVenta detalle : venta.getDetallesVentas()) {
            
            // Solo devolvemos stock si es un producto físico (Los servicios no tienen stock)
            if (detalle.getProducto() != null) {
                Producto prod = detalle.getProducto();
                
                // Sumamos la cantidad que se había vendido de vuelta al stock total
                prod.setStockTotal(prod.getStockTotal().add(detalle.getCantidad()));
                
                // Guardamos el producto con su stock restaurado
                productoRepository.save(prod);
            }
        }

        // 3. Cambiar el estado de la venta a inactivo/anulado
        venta.setEstado(false);
        ventaRepository.save(venta);

    }
}