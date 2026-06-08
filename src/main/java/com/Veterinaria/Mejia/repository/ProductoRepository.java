package com.Veterinaria.Mejia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.Veterinaria.Mejia.models.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // JPQL: Trae los productos de una categoría específica que estén activos
    @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId AND p.estado = true")
    List<Producto> buscarActivosPorCategoriaJPQL(@Param("categoriaId") Integer categoriaId);

    // JPQL: Dispara la alerta de inventario crítico (Stock Total <= Stock Mínimo)
    @Query("SELECT p FROM Producto p WHERE p.stockTotal <= p.stockMinimo AND p.estado = true")
    List<Producto> buscarProductosStockCriticoJPQL();

    // =========================================================================
    // QUERY JPQL ORIGINAL: Filtra por Categoría y por la Especie de la Mascota
    // =========================================================================
    @Query("SELECT p FROM Producto p WHERE p.categoria.id = :categoriaId AND p.especie.id = :especieId")
    List<Producto> buscarPorCategoriaYEspecieJPQL(@Param("categoriaId") Integer categoriaId, 
                                                  @Param("especieId") Integer especieId);

    // =========================================================================
    // NUEVA QUERY JPQL: Buscador dinámico con filtros opcionales para Inventario
    // =========================================================================
    @Query("SELECT p FROM Producto p WHERE " +
           "(:nombre IS NULL OR :nombre = '' OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) AND " +
           "(:especieId IS NULL OR " +
           "  (:especieId = 0 AND p.especie IS NULL) OR " +
           "  (p.especie.id = :especieId))")
    List<Producto> buscarYFiltrarInventarioJPQL(
            @Param("nombre") String nombre,
            @Param("categoriaId") Integer categoriaId,
            @Param("especieId") Integer especieId);
}