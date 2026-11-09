package com.springapi.dev;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.springapi.department.Department;
import com.springapi.department.DepartmentRepository;
import com.springapi.department.DepartmentStatus;
import com.springapi.hospital.Hospital;
import com.springapi.hospital.HospitalRepository;
import com.springapi.hospital.HospitalType;
import com.springapi.user.User;
import com.springapi.user.UserRepository;
import com.springapi.user.UserRole;

@Component
@Profile("dev")
@ConditionalOnProperty(
        name = "app.dev-seed.enabled",
        havingValue = "true")
public class DevelopmentDataSeeder implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(DevelopmentDataSeeder.class);
    private static final String HOSPITAL_REGISTRATION_NUMBER = "DEV-MQ-001";
    private static final List<DepartmentSeed> DEPARTMENTS = List.of(
            new DepartmentSeed("General Practice", "GEN"),
            new DepartmentSeed("Emergency", "ER"),
            new DepartmentSeed("Maternity", "MAT"));
    private static final List<UserSeed> USERS = List.of(
            new UserSeed("dev-system-admin", "dev-system-admin@example.test",
                    "Development System Admin", UserRole.SYSTEM_ADMIN, false),
            new UserSeed("dev-hospital-admin", "dev-hospital-admin@example.test",
                    "Development Hospital Admin", UserRole.HOSPITAL_ADMIN, true),
            new UserSeed("dev-doctor", "dev-doctor@example.test",
                    "Development Doctor", UserRole.DOCTOR, true),
            new UserSeed("dev-receptionist", "dev-receptionist@example.test",
                    "Development Receptionist", UserRole.RECEPTIONIST, true),
            new UserSeed("dev-medical-staff", "dev-medical-staff@example.test",
                    "Development Medical Staff", UserRole.MEDICAL_STAFF, true),
            new UserSeed("dev-responder", "dev-responder@example.test",
                    "Development Responder", UserRole.RESPONDER, true),
            new UserSeed("dev-patient", "dev-patient@example.test",
                    "Development Patient", UserRole.PATIENT, true));

    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DevelopmentDataSeeder(
            HospitalRepository hospitalRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${MEDIQUEUE_DEV_SEED_PASSWORD:}") String seedPassword) {
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateSeedPassword();

        Hospital hospital = getOrCreateHospital();
        seedDepartments(hospital);
        seedUsers(hospital);
        log.info("Development seed data is ready.");
    }

    private void validateSeedPassword() {
        int passwordBytes = seedPassword.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 12 || passwordBytes > 72) {
            throw new IllegalStateException(
                    "Set MEDIQUEUE_DEV_SEED_PASSWORD to a development-only "
                            + "password between 12 and 72 UTF-8 bytes.");
        }
    }

    private Hospital getOrCreateHospital() {
        return hospitalRepository
                .findByRegistrationNumber(HOSPITAL_REGISTRATION_NUMBER)
                .orElseGet(() -> {
                    Hospital hospital = new Hospital();
                    hospital.setName("MediQueue Development Hospital");
                    hospital.setRegistrationNumber(
                            HOSPITAL_REGISTRATION_NUMBER);
                    hospital.setHospitalType(HospitalType.PUBLIC);
                    hospital.setTown("Development");
                    return hospitalRepository.save(hospital);
                });
    }

    private void seedDepartments(Hospital hospital) {
        for (DepartmentSeed seed : DEPARTMENTS) {
            Department department = departmentRepository
                    .findByHospitalIdAndCodeIgnoreCase(
                            hospital.getId(), seed.code())
                    .orElseGet(() -> {
                        Department created = new Department();
                        created.setHospital(hospital);
                        created.setName(seed.name());
                        created.setCode(seed.code());
                        created.setStatus(DepartmentStatus.ACTIVE);
                        return departmentRepository.save(created);
                    });

            if (!department.getName().equals(seed.name())) {
                throw new IllegalStateException(
                        "Development department code conflicts with existing data: "
                                + seed.code());
            }
        }
    }

    private void seedUsers(Hospital hospital) {
        for (UserSeed seed : USERS) {
            User existing = userRepository.findByUsername(seed.username())
                    .orElse(null);
            Long expectedHospitalId = seed.hospitalAssigned()
                    ? hospital.getId()
                    : null;

            if (existing != null) {
                Long actualHospitalId = existing.getHospital() == null
                        ? null
                        : existing.getHospital().getId();
                if (existing.getRole() != seed.role()
                        || !java.util.Objects.equals(
                                actualHospitalId, expectedHospitalId)) {
                    throw new IllegalStateException(
                            "Development account conflicts with existing user: "
                                    + seed.username());
                }
                continue;
            }

            if (userRepository.findByEmail(seed.email()).isPresent()) {
                throw new IllegalStateException(
                        "Development account email is already in use: "
                                + seed.email());
            }

            User user = new User();
            user.setUsername(seed.username());
            user.setEmail(seed.email());
            user.setFullName(seed.fullName());
            user.setPasswordHash(passwordEncoder.encode(seedPassword));
            user.setRole(seed.role());
            user.setEnabled(true);
            if (seed.hospitalAssigned()) {
                user.setHospital(hospital);
            }
            userRepository.save(user);
        }
    }

    private record DepartmentSeed(String name, String code) {
    }

    private record UserSeed(
            String username,
            String email,
            String fullName,
            UserRole role,
            boolean hospitalAssigned) {
    }
}
