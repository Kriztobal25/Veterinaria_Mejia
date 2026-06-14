package com.Veterinaria.Mejia.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Veterinaria.Mejia.dto.ItemCarritoDTO;
import com.Veterinaria.Mejia.dto.VentaRequestDTO;
import com.Veterinaria.Mejia.models.Cliente;
import com.Veterinaria.Mejia.models.DetalleVenta;
import com.Veterinaria.Mejia.models.Producto;
import com.Veterinaria.Mejia.models.Servicio;
import com.Veterinaria.Mejia.models.Venta;
import com.Veterinaria.Mejia.repository.ClienteRepository;
import com.Veterinaria.Mejia.repository.ProductoRepository;
import com.Veterinaria.Mejia.repository.ServicioRepository;
import com.Veterinaria.Mejia.repository.VentaRepository;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository; 
    
    // Inyectamos el repositorio de servicios para procesar el DTO
    @Autowired
    private ServicioRepository servicioRepository;

    @Transactional(rollbackFor = Exception.class)
    public Venta procesarVentaTransaccional(VentaRequestDTO request) {
        
        // 0. REGLA DE NEGOCIO: Evitar ventas en blanco
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("La venta no puede procesarse porque el carrito está vacío.");
        }
        
        // Inicializamos la cabecera de la venta con los datos del Request
        Venta venta = new Venta();
        venta.setFechaEmision(LocalDateTime.now());
        venta.setTotalVenta(request.getTotal());
        venta.setTipoPago(request.getTipoPago());
        venta.setEstado(true);
        venta.setDetallesVentas(new ArrayList<>());

        // 🔥 CORRECCIÓN: Le asignamos el ID del cajero (obligatorio para la BD)
        venta.setUsuarioId(1);

        // =====================================================================
        // 1. LÓGICA INTELIGENTE DEL CLIENTE 
        // =====================================================================
        String dniIngresado = request.getClienteDni();
        String nombreIngresado = request.getClienteNombre();

        if (dniIngresado != null && !dniIngresado.trim().isEmpty()) {
            Optional<Cliente> clienteExistente = clienteRepository.findByDniJPQL(dniIngresado); 
            
            if (clienteExistente.isPresent()) {
                venta.setClienteId(clienteExistente.get().getId()); 
            } else {
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setDni(dniIngresado);
                nuevoCliente.setNombre(nombreIngresado != null && !nombreIngresado.trim().isEmpty() ? nombreIngresado : "Cliente sin nombre");
                
                Cliente clienteGuardado = clienteRepository.save(nuevoCliente);
                venta.setClienteId(clienteGuardado.getId()); 
            }
        } else {
            // Público General
            venta.setClienteId(null); 
        }

        // =====================================================================
        // 2. REGLA DE NEGOCIO: Evaluación automática del monto de S/. 5.00
        // =====================================================================
        if (venta.getTotalVenta().compareTo(new BigDecimal("5.00")) >= 0) {
            venta.setTipoComprobante("Boleta");
            venta.setSerie("B001");
        } else {
            venta.setTipoComprobante("Ticket");
            venta.setSerie("T001");
        }

        // =====================================================================
        // 3. CONTROL AUTÓNOMO DE CORRELATIVOS VIA JPQL
        // =====================================================================
        Integer ultimoCorrelativo = ventaRepository.obtenerMaximoCorrelativoJPQL(venta.getSerie());
        int nuevoCorrelativo = (ultimoCorrelativo == null) ? 1 : ultimoCorrelativo + 1;
        venta.setCorrelativo(nuevoCorrelativo);

        // =====================================================================
        // 4. LECTURA DEL CARRITO, INVENTARIO Y VALIDACIÓN DE SERVICIOS
        // =====================================================================
        Set<Integer> serviciosCobrados = new HashSet<>();

        for (ItemCarritoDTO item : request.getItems()) {
            
            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setCantidad(item.getCantidad());

            // A) LÓGICA PARA SERVICIOS CLÍNICOS
            if ("SERVICIO".equalsIgnoreCase(item.getTipo())) {
                Servicio serv = servicioRepository.findById(item.getIdItem())
                        .orElseThrow(() -> new RuntimeException("Servicio no encontrado."));
                
                // Evita duplicidad en la misma boleta
                if (!serviciosCobrados.add(serv.getId())) {
                    throw new IllegalArgumentException("Error: No puedes registrar el mismo servicio más de una vez en la misma boleta.");
                }
                detalle.setServicio(serv);
                
                // Cálculo seguro en el backend
                detalle.setPrecioUnitario(serv.getPrecioServicio());
                detalle.setSubtotal(serv.getPrecioServicio().multiply(item.getCantidad()));

            // B) LÓGICA PARA PRODUCTOS FÍSICOS (Sacos, Farmacia, etc.)
            } else if ("PRODUCTO".equalsIgnoreCase(item.getTipo())) {
                Producto prod = productoRepository.findById(item.getIdItem())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado en el catálogo."));

                // 1. VALIDACIÓN DE PRODUCTOS "SUELTOS" vs ENTEROS
                // Si se vende por 'unidad', rechazamos tajantemente que traiga fracciones (decimales)
                if ("unidad".equalsIgnoreCase(prod.getTipoUnidad()) && item.getCantidad().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                    throw new IllegalArgumentException("El producto '" + prod.getNombre() + "' no se puede vender fraccionado (solo admite unidades enteras).");
                }

                // 2. CÁLCULO DE PRECIO PROPORCIONAL
                // Multiplica la cantidad (entera o decimal) por el valor unitario original. Ej: 0.5kg * 100 = 50.00
                detalle.setPrecioUnitario(prod.getPrecioVentaActual());
                detalle.setSubtotal(prod.getPrecioVentaActual().multiply(item.getCantidad()));

                // Validación de seguridad física antes de descontar (Soporta decimales Ej: 0.5kg)
                if (prod.getStockTotal().compareTo(item.getCantidad()) < 0) {
                    throw new RuntimeException("Stock insuficiente para el producto: " + prod.getNombre());
                }

                // Restamos la cantidad exacta vendida
                prod.setStockTotal(prod.getStockTotal().subtract(item.getCantidad()));
                productoRepository.save(prod);
                
                detalle.setProducto(prod);
            } else {
                // C) PREVENCIÓN DE ERRORES SI SE ENVÍA UN TIPO INVÁLIDO
                throw new IllegalArgumentException("Tipo de ítem desconocido: '" + item.getTipo() + "'. Solo se permite SERVICIO o PRODUCTO.");
            }

            venta.getDetallesVentas().add(detalle);
        }

        // 5. Guardamos de forma definitiva la cabecera y todos sus detalles en cascada
        return ventaRepository.save(venta);
    }

    // =========================================================================
    // MÉTODOS DE LECTURA Y ANULACIÓN
    // =========================================================================
    public Venta buscarPorId(Integer id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transacción de venta no encontrada."));
    }

    public List<Venta> listarTodas() {
        return ventaRepository.findAll();
    }
    
    @Transactional
    public void anularVenta(Integer idVenta) {
        Venta venta = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new RuntimeException("Transacción de venta no encontrada."));

        if (!venta.getEstado()) {
            throw new IllegalArgumentException("Esta venta ya se encuentra anulada.");
        }

        // Devolver el stock de los productos al inventario
        for (DetalleVenta detalle : venta.getDetallesVentas()) {
            if (detalle.getProducto() != null) {
                Producto prod = detalle.getProducto();
                prod.setStockTotal(prod.getStockTotal().add(detalle.getCantidad()));
                productoRepository.save(prod);
            }
        }

        venta.setEstado(false);
        ventaRepository.save(venta);
    }
}