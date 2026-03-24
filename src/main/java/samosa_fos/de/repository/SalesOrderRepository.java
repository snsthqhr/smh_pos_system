package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.SalesOrder;

import java.time.LocalDate;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    // 특정 고객의 판매 전표 조회
    List<SalesOrder> findByCustomerId(Long customerId);

    // 특정 날짜의 판매 전표 조회
    List<SalesOrder> findBySalesDate(LocalDate salesDate);

    // 특정 고객 + 특정 날짜의 판매 전표 조회
    List<SalesOrder> findByCustomerIdAndSalesDate(Long customerId, LocalDate salesDate);

    // 활성화된 판매 전표만 조회
    List<SalesOrder> findByActiveTrue();

    //고객 번호와 작업현장으로 조회
    List<SalesOrder> findByCustomerIdAndJobSiteId(Long customerId, Long jobsiteId);
}
