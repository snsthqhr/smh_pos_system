package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.CustomerPrice;

import java.util.List;
import java.util.Optional;

public interface CustomerPriceRepository extends JpaRepository<CustomerPrice,Long> {

    // 고객 + 상품 기준 가격 조회
    // (현장 없는 기본 고객 가격)
    Optional<CustomerPrice> findByCustomerIdAndProductIdAndJobSiteIdIsNullAndActiveTrue(Long customerId, Long productId);

    // 고객 + 현장 + 상품 기준 가격 조회
    // (현장별 특가)
    Optional<CustomerPrice> findByCustomerIdAndProductIdAndJobSiteIdAndActiveTrue(Long customerId, Long productId, Long jobSiteId);

    // 특정 고객의 모든 가격 정책 조회
    List<CustomerPrice> findByCustomerIdAndActiveTrue(Long customerId);


}
