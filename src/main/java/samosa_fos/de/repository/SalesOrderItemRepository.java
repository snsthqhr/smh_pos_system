package samosa_fos.de.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.SalesOrderItem;

import java.util.List;

public interface SalesOrderItemRepository extends JpaRepository<SalesOrderItem, Long> {

    // 특정 판매 전표에 속한 상품 목록 조회
    List<SalesOrderItem> findBySalesOrderId(Long salesOrderId);

    // 특정 상품이 포함된 판매 항목 조회
    List<SalesOrderItem> findByProductId(Long productId);

    // 활성화된 판매 항목만 조회
    List<SalesOrderItem> findByActiveTrue();

    // 여러 판매 전표의 품목 조회
    List<SalesOrderItem> findBySalesOrderIdIn(List<Long> salesOrderIds);

}
