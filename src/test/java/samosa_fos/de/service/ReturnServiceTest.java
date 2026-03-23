package samosa_fos.de.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
public class ReturnServiceTest {

    @Autowired
    ReturnService returnService;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Test
    @DisplayName("반품 정상 처리 테스트")
    void returnSuccess() {

        //given
        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setTaxPolicy("NO_TAX");
        order.setTotalNetAmount(100000);
        order.setTotalTaxAmount(0);
        order.setTotalAmount(100000);
        order.setActive(true);

        SalesOrder savedOrder = salesOrderRepository.save(order);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(10L);
        item.setQuantity(5);
        item.setUnitPrice(20000);
        item.setReturnQuantity(0);
        item.setSupplyPrice(100000);
        item.setTaxPrice(0);
        item.setTotalPrice(100000);
        item.setActive(true);

        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        //when

        returnService.addReturnQuantity(savedItem.getId(),2);

        //then

        SalesOrderItem findItem = salesOrderItemRepository.findById(savedItem.getId()).orElseThrow();

        assertThat(findItem.getReturnQuantity()).isEqualTo(2);
        assertThat(findItem.getSupplyPrice()).isEqualTo(60000); // 3개 남음
        assertThat(findItem.getTotalPrice()).isEqualTo(60000);

        SalesOrder findOrder = salesOrderRepository.findById(savedOrder.getId()).orElseThrow();
        assertThat(findOrder.getTotalAmount()).isEqualTo(60000);


    }


    @Test
    @DisplayName("반품 누적 테스트")

}
