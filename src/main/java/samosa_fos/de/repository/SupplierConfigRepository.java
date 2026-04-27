package samosa_fos.de.repository;

import samosa_fos.de.domain.SupplierConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierConfigRepository {

    Optional<SupplierConfig> findFirstByActiveTrueOrderByIdDesc();
}
