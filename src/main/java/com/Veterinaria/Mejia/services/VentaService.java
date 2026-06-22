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
import com.Veterinaria.Mejia.models.Usuario;
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
        Usuario cajero = new Usuario();
        cajero.setId(1);
        venta.setUsuario(cajero);

        // =====================================================================
        // 1. LÓGICA INTELIGENTE DEL CLIENTE 
        // =====================================================================
        String dniIngresado = request.getClienteDni();
        String nombreIngresado = request.getClienteNombre();

        if (dniIngresado != null && !dniIngresado.trim().isEmpty()) {
            Optional<Cliente> clienteExistente = clienteRepository.findByDniJPQL(dniIngresado); 
            
            if (clienteExistente.isPresent()) {
                venta.setCliente(clienteExistente.get()); 
            } else {
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setDni(dniIngresado);
                nuevoCliente.setNombre(nombreIngresado != null && !nombreIngresado.trim().isEmpty() ? nombreIngresado : "Cliente sin nombre");
                
                Cliente clienteGuardado = clienteRepository.save(nuevoCliente);
                venta.setCliente(clienteGuardado); 
            }
        } else {
            // Público General
            venta.setCliente(null); 
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

            // B) LÓGICA PARA PRODUCTOS FÍSICOS (ENTERO O FRACCIONADO)
            } else if ("PRODUCTO_ENTERO".equalsIgnoreCase(item.getTipo()) || "PRODUCTO_FRACCIONADO".equalsIgnoreCase(item.getTipo())) {
                Producto prod = productoRepository.findById(item.getIdItem())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado en el catálogo."));

                boolean esFraccionado = "PRODUCTO_FRACCIONADO".equalsIgnoreCase(item.getTipo());
                
                if (esFraccionado) {
                    if (prod.getPermiteFraccionamiento() == null || !prod.getPermiteFraccionamiento()) {
                        throw new IllegalArgumentException("El producto '" + prod.getNombre() + "' no permite venta fraccionada.");
                    }
                    
                    BigDecimal cantidadRequerida = item.getCantidad();
                    
                    // Si lo requerido es mayor al stock abierto, rompemos sacos cerrados hasta que alcance
                    while (prod.getStockAbierto().compareTo(cantidadRequerida) < 0) {
                        if (prod.getStockCerrado() == null || prod.getStockCerrado() <= 0) {
                            throw new RuntimeException("Stock insuficiente para venta suelta de: " + prod.getNombre() + ". No hay envases cerrados disponibles para abrir.");
                        }
                        prod.setStockCerrado(prod.getStockCerrado() - 1); // Restamos 1 envase sellado
                        prod.setStockAbierto(prod.getStockAbierto().add(prod.getContenidoPorEnvase())); // Volcamos el contenido a granel
                    }
                    
                    prod.setStockAbierto(prod.getStockAbierto().subtract(cantidadRequerida));
                    detalle.setPrecioUnitario(prod.getPrecioPorFraccion());
                    detalle.setSubtotal(prod.getPrecioPorFraccion().multiply(item.getCantidad()));
                    
                } else {
                    // Lógica para productos en modo ENTERO
                    if (item.getCantidad().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                        throw new IllegalArgumentException("El producto '" + prod.getNombre() + "' en modo entero no se puede vender con decimales.");
                    }
                    
                    int cantidadEntera = item.getCantidad().intValue();
                    if (prod.getStockCerrado() == null || prod.getStockCerrado() < cantidadEntera) {
                        throw new RuntimeException("Stock de envases cerrados insuficiente para: " + prod.getNombre());
                    }
                    
                    prod.setStockCerrado(prod.getStockCerrado() - cantidadEntera);
                    detalle.setPrecioUnitario(prod.getPrecioVentaActual());
                    detalle.setSubtotal(prod.getPrecioVentaActual().multiply(item.getCantidad()));
                }
                
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
    
    // Obtener solo las últimas 10 ventas
    public List<Venta> listarUltimas10Ventas() {
        return ventaRepository.findTop10ByOrderByFechaEmisionDesc();
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