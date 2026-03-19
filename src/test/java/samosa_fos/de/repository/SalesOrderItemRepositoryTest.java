package samosa_fos.de.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrderItem;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
class SalesOrderItemRepositoryTest {

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Test
    @DisplayName("세일즈 오더 아이템 저장 테스트")
    void saveSalesOrderItem() {

        // given
        SalesOrderItem salesOrderItem = new SalesOrderItem();
        salesOrderItem.setSalesOrderId(1L);     // 어떤 판매 전표에 속하는지
        salesOrderItem.setProductId(10L);       // 어떤 상품인지
        salesOrderItem.setQuantity(2);          // 수량
        salesOrderItem.setUnitPrice(52000);     // 판매 당시 단가
        salesOrderItem.setSupplyPrice(104000); // 공급가 합계
        salesOrderItem.setTaxPrice(0);         // 부가세 없음
        salesOrderItem.setTotalPrice(104000);  // 최종 금액
        salesOrderItem.setActive(true);

        // when
        SalesOrderItem savedItem = salesOrderItemRepository.save(salesOrderItem);

        // then
        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getSalesOrderId()).isEqualTo(1L);
        assertThat(savedItem.getProductId()).isEqualTo(10L);
        assertThat(savedItem.getQuantity()).isEqualTo(2);
        assertThat(savedItem.getUnitPrice()).isEqualTo(52000);
        assertThat(savedItem.getSupplyPrice()).isEqualTo(104000);
        assertThat(savedItem.getTaxPrice()).isEqualTo(0);
        assertThat(savedItem.getTotalPrice()).isEqualTo(104000);
        assertThat(savedItem.getActive()).isTrue();
    }

    void
}