package samosa_fos.de.repository;

import jakarta.persistence.ExcludeDefaultListeners;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SpringBootTest
@Transactional
public class SalesOrderAndItemRepositoryTest {

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Test
    @DisplayName("판매 전표 1건과 판매 상품 여러 건 저장 테스트")
    void saveSalesOrderWithItems() {

        //given

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(1L);
        salesOrder.setJobSiteId(100L);
        salesOrder.setPaymentType("CARD");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setMemo("배건우 사장 강릉 건");
        salesOrder.setActive(true);

        //when
        //전표 저장
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        // 상품 1
        SalesOrderItem item1 = new SalesOrderItem();
        item1.setSalesOrderId(savedOrder.getId());
        item1.setProductId(10L);
        item1.setQuantity(2);
        item1.setUnitPrice(52000);
        item1.setSupplyPrice(104000);
        item1.setTaxPrice(0);
        item1.setTotalPrice(104000);
        item1.setActive(true);

        // 상품 2
        SalesOrderItem item2 = new SalesOrderItem();
        item2.setSalesOrderId(savedOrder.getId());
        item2.setProductId(11L);
        item2.setQuantity(1);
        item2.setUnitPrice(28000);
        item2.setSupplyPrice(28000);
        item2.setTaxPrice(0);
        item2.setTotalPrice(28000);
        item2.setActive(true);

        // 상품 3
        SalesOrderItem item3 = new SalesOrderItem();
        item3.setSalesOrderId(savedOrder.getId());
        item3.setProductId(12L);
        item3.setQuantity(3);
        item3.setUnitPrice(5000);
        item3.setSupplyPrice(15000);
        item3.setTaxPrice(0);
        item3.setTotalPrice(15000);
        item3.setActive(true);

        //when

        salesOrderItemRepository.save(item1);
        salesOrderItemRepository.save(item2);
        salesOrderItemRepository.save(item3);

        //then
        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(savedOrder.getId());

        assertThat(savedOrder.getId()).isNotNull();
        assertThat(items).hasSize(3);

        assertThat(items).extracting("productId")
                .containsExactlyInAnyOrder(10L,11L,12L);

        assertThat(items).allMatch(item ->item.getSalesOrderId().equals(savedOrder.getId()));

        SalesOrderItem findItem1 = items.stream()
                .filter(item ->item.getProductId().equals(10L))
                .findFirst()
                .orElseThrow();

        assertThat(findItem1.getQuantity()).isEqualTo(2);
        assertThat(findItem1.getUnitPrice()).isEqualTo(52000);
        assertThat(findItem1.getSupplyPrice()).isEqualTo(104000);
        assertThat(findItem1.getTaxPrice()).isEqualTo(0);
        assertThat(findItem1.getTotalPrice()).isEqualTo(104000);

    }


}
