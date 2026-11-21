package com.springapi.user;

import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	Optional<User> findBySupabaseUserId(UUID supabaseUserId);

	Optional<User> findByIdAndRole(Long id, UserRole role);

	Optional<User> findByIdAndRoleAndHospitalId(
			Long id,
			UserRole role,
			Long hospitalId);

	Page<User> findAllByRole(UserRole role, Pageable pageable);

	@Query("""
			select u from User u
			where (:role is null or u.role = :role)
			  and (:enabled is null or u.enabled = :enabled)
			  and (:hospitalId is null or u.hospital.id = :hospitalId)
			  and (:search = ''
			       or lower(u.username) like lower(concat('%', :search, '%'))
			       or lower(u.email) like lower(concat('%', :search, '%'))
			       or lower(u.fullName) like lower(concat('%', :search, '%')))
			""")
	Page<User> findFiltered(
			@Param("role") UserRole role,
			@Param("enabled") Boolean enabled,
			@Param("hospitalId") Long hospitalId,
			@Param("search") String search,
			Pageable pageable);

	@Query("""
			select u from User u
			where u.role = com.springapi.user.UserRole.PATIENT
			  and (:search = ''
			       or lower(u.username) like lower(concat('%', :search, '%'))
			       or lower(u.email) like lower(concat('%', :search, '%'))
			       or lower(u.fullName) like lower(concat('%', :search, '%')))
			""")
	Page<User> findPatients(
			@Param("search") String search,
			Pageable pageable);

	Page<User> findAllByHospitalIdAndRole(
			Long hospitalId,
			UserRole role,
			Pageable pageable);

	Page<User> findAllByHospitalIdAndRoleAndEnabledTrueAndOnDutyTrue(
			Long hospitalId,
			UserRole role,
			Pageable pageable);

	Page<User> findAllByHospitalIdAndRoleIn(
			Long hospitalId,
			Collection<UserRole> roles,
			Pageable pageable);

	@Query("""
			select u from User u
			where u.hospital.id = :hospitalId
			  and u.role = com.springapi.user.UserRole.DOCTOR
			  and (:onDuty is null or u.onDuty = :onDuty)
			""")
	Page<User> findDoctors(
			@Param("hospitalId") Long hospitalId,
			@Param("onDuty") Boolean onDuty,
			Pageable pageable);

	Optional<User> findByIdAndHospitalIdAndRole(
			Long id,
			Long hospitalId,
			UserRole role);

	Optional<User> findByIdAndHospitalIdAndRoleIn(
			Long id,
			Long hospitalId,
			Collection<UserRole> roles);

	long countByHospitalIdAndRoleAndOnDutyTrue(
			Long hospitalId,
			UserRole role);

	@Query("select u.hospital.id from User u where u.username = :username and u.hospital is not null")
	Optional<Long> findHospitalIdByUsername(@Param("username") String username);

}
