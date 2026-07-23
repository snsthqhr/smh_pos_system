package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.ArTx;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ArTxRepository extends JpaRepository<ArTx, Long> {

    // 특정 고객의 미수금 원장 조회
    List<ArTx> findByCustomerId(Long customerId);

    // 특정 고객의 활성 미수금 원장 조회
    List<ArTx> findByCustomerIdAndActiveTrue(Long customerId);

    // 특정 기간의 미수금 원장 조회
    List<ArTx> findByTxDateBetween(LocalDate startDate, LocalDate endDate);

    // 특정 고객의 특정 기간 미수금 원장 조회
    List<ArTx> findByCustomerIdAndTxDateBetween(Long customerId, LocalDate startDate, LocalDate endDate);

    // 특정 전표와 연결된 미수금 원장 조회
    List<ArTx> findBySalesOrderId(Long salesOrderId);

    // 전표 취소 시 활성 미수 흐름을 확인하고 비활성화하는 데 사용한다.
    List<ArTx> findBySalesOrderIdAndActiveTrue(Long salesOrderId);

    // 특정 고객 + 거래유형 기준 조회
    List<ArTx> findByCustomerIdAndTxType(Long customerId, String txType);

    List<ArTx> findByCustomerIdAndTxDateBeforeAndActiveTrue(Long customerId, LocalDate Date);

    Optional<ArTx> findBySalesOrderIdAndTxTypeAndActiveTrue(Long salesOrderId, String txType);
}
