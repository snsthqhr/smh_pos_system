package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.Payment;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 특정 고객의 수금 내역 조회
    List<Payment> findByCustomerId(Long customerId);

    // 특정 고객의 활성 수금 내역 조회
    List<Payment> findByCustomerIdAndActiveTrue(Long customerId);

    // 특정 기간의 수금 내역 조회
    List<Payment> findByPaymentDateBetween(LocalDate startDate, LocalDate endDate);

    // 특정 고객의 특정 기간 수금 내역 조회
    List<Payment> findByCustomerIdAndPaymentDateBetween(Long customerId, LocalDate startDate, LocalDate endDate);

    // 특정 판매 전표와 연결된 수금 내역 조회
    List<Payment> findBySalesOrderId(Long salesOrderId);

    // 판매 수정 시 전표에 직접 연결된 활성 수금액 합계와 즉시결제 Payment 갱신에 사용한다.
    List<Payment> findBySalesOrderIdAndActiveTrue(Long salesOrderId);

    boolean existsBySalesOrderIdAndActiveTrue(Long salesOrderId);
}
