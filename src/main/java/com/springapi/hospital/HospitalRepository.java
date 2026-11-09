
package com.springapi.hospital;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    Optional<Hospital> findByRegistrationNumber(String registrationNumber);

    @Query("""
            select h from Hospital h
            where (:search is null
                   or lower(h.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(h.county, '')) like lower(concat('%', :search, '%'))
                   or lower(coalesce(h.town, '')) like lower(concat('%', :search, '%')))
              and (:status is null or h.status = :status)
            """)
    Page<Hospital> findFiltered(
            @Param("search") String search,
            @Param("status") HospitalStatus status,
            Pageable pageable);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(
            String registrationNumber,
            Long id
    );
}
