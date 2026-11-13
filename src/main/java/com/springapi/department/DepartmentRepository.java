
package com.springapi.department;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByHospitalId(Long hospitalId);

    Page<Department> findByHospitalIdAndStatus(
            Long hospitalId,
            DepartmentStatus status,
            Pageable pageable);

    @Query("""
            select d from Department d
            where d.hospital.id = :hospitalId
              and (:search = ''
                   or lower(d.name) like lower(concat('%', :search, '%'))
                   or lower(d.code) like lower(concat('%', :search, '%')))
              and (:status is null or d.status = :status)
            """)
    Page<Department> findFiltered(
            @Param("hospitalId") Long hospitalId,
            @Param("search") String search,
            @Param("status") DepartmentStatus status,
            Pageable pageable);

    Optional<Department> findByHospitalIdAndId(Long hospitalId, Long id);

    Optional<Department> findByHospitalIdAndCodeIgnoreCase(
            Long hospitalId,
            String code);

    @Query("select d.hospital.id from Department d where d.id = :departmentId")
    Optional<Long> findHospitalIdById(
            @Param("departmentId") Long departmentId);

    boolean existsByHospitalIdAndNameIgnoreCase(
            Long hospitalId,
            String name
    );

    boolean existsByHospitalIdAndCodeIgnoreCase(
            Long hospitalId,
            String code
    );

    boolean existsByHospitalIdAndNameIgnoreCaseAndIdNot(
            Long hospitalId,
            String name,
            Long id
    );

    boolean existsByHospitalIdAndCodeIgnoreCaseAndIdNot(
            Long hospitalId,
            String code,
            Long id
    );

    boolean existsByHospitalId(Long hospitalId);
}
