package samosa_fos.de.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.ReturnRequest;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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

        ReturnRequest request = new ReturnRequest();
        request.setReturnQuantity(2);
        request.setSalesOrderItemId(item.getId());
        request.setSalesOrderId(item.getId());

        returnService.addReturnQuantity(request);

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
    void returnAccumlate() {

        //given
        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setTaxPolicy("NO_TAX");
        order.setTotalAmount(100000);
        order.setActive(true);

        SalesOrder savedOrder = salesOrderRepository.save(order);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setQuantity(5);
        item.setUnitPrice(20000);
        item.setReturnQuantity(0);
        item.setSupplyPrice(80000);
        item.setTotalPrice(80000);
        item.setActive(true);

        SalesOrderItem savedItem = salesOrderItemRepository.save(item);



        //when

        ReturnRequest request = new ReturnRequest();
        request.setReturnQuantity(3);
        request.setSalesOrderId(savedItem.getSalesOrderId());
        request.setSalesOrderItemId(savedItem.getId());
        returnService.addReturnQuantity(request);

        //then
        SalesOrderItem finditem = salesOrderItemRepository.findById(savedItem.getId()).orElseThrow();

        assertThat(finditem.getReturnQuantity()).isEqualTo(3);

        ReturnRequest request2 = new ReturnRequest();
        request.setReturnQuantity(1);
        request.setSalesOrderId(savedItem.getSalesOrderId());
        request.setSalesOrderItemId(savedItem.getId());
        returnService.addReturnQuantity(request);

        returnService.addReturnQuantity(request);
        assertThat(finditem.getReturnQuantity()).isEqualTo(4);
    }

    @Test
    @DisplayName("반품 수량 초과 예외 테스트")
    void return_over_exception() {

        // given
        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setActive(true);

        SalesOrder savedOrder = salesOrderRepository.save(order);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(10L);
        item.setQuantity(3);
        item.setReturnQuantity(0);
        item.setActive(true);


        SalesOrderItem savedItem = salesOrderItemRepository.save(item);


        // when & then

        ReturnRequest request = new ReturnRequest();
        request.setReturnQuantity(4);
        request.setSalesOrderId(savedItem.getSalesOrderId());
        request.setSalesOrderItemId(savedItem.getId());
        returnService.addReturnQuantity(request);

        assertThatThrownBy(() ->
                returnService.addReturnQuantity(request)
        ).isInstanceOf(IllegalArgumentException.class);
    }

}
