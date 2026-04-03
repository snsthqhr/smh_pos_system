package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.SalesOrder;

import java.time.LocalDate;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    // 특정 고객의 전체 판매 전표 조회
    List<SalesOrder> findByCustomerId(Long customerId);

    // 특정 고객의 활성 판매 전표 조회
    List<SalesOrder> findByCustomerIdAndActiveTrue(Long customerId);

    // 특정 고객의 기간별 판매 전표 조회
    List<SalesOrder> findByCustomerIdAndSalesDateBetween(Long customerId, LocalDate startDate, LocalDate endDate);

    // 특정 고객의 기간별 활성 판매 전표 조회
    List<SalesOrder> findByCustomerIdAndSalesDateBetweenAndActiveTrue(Long customerId, LocalDate startDate, LocalDate endDate);

    // 특정 날짜의 전체 판매 전표 조회
    List<SalesOrder> findBySalesDate(LocalDate salesDate);

    // 특정 날짜의 전체 활성 판매 전표 조회
    List<SalesOrder> findBySalesDateAndActiveTrue(LocalDate salesDate);

    // 특정 기간의 전체 판매 전표 조회
    List<SalesOrder> findBySalesDateBetween(LocalDate startDate, LocalDate endDate);

    // 특정 기간의 전체 활성 판매 전표 조회
    List<SalesOrder> findBySalesDateBetweenAndActiveTrue(LocalDate startDate, LocalDate endDate);
}
