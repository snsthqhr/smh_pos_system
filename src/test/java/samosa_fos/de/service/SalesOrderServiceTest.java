package samosa_fos.de.service;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class SalesOrderServiceTest {

    @Autowired
    SalesOrderService salesOrderService;


    @Autowired
    SalesOrderRepository salesOrderRepository;


    @Autowired
    private SalesOrderItemRepository salesOrderItemRepository;

    @Test
    @DisplayName("판매 전표 생성 서비스 테스트 - 부가세 없음")
    void createSalesOrder_noTax() {


        //given
        CreateSalesOrderItemRequest item1 = new CreateSalesOrderItemRequest();
        item1.setProductId(10L);
        item1.setQuantity(2);
        item1.setUnitPrice(52000);

        CreateSalesOrderItemRequest item2 = new CreateSalesOrderItemRequest();
        item2.setProductId(11L);
        item2.setQuantity(1);
        item2.setUnitPrice(28000);

        List<CreateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);

        CreateSalesOrderRequest request = new CreateSalesOrderRequest();
        request.setItems(items);
        request.setCustomerId(1L);
        request.setJobSiteId(100L);
        request.setTaxPolicy("NO_TAX");
        request.setPaymentType("CARD");
        request.setMemo("서비스 테스트");

        //when

        SalesOrder savedOrder = salesOrderService.createSalesOrder(request);

        //then

        assertThat(savedOrder.getId()).isNotNull();
        assertThat(savedOrder.getCustomerId()).isEqualTo(1L);
        assertThat(savedOrder.getJobSiteId()).isEqualTo(100L);
        assertThat((savedOrder.getMemo())).isEqualTo("서비스 테스트");
        assertThat(savedOrder.getPaymentType()).isEqualTo("CARD");
        assertThat(savedOrder.getTaxPolicy()).isEqualTo("NO_TAX");

        //공급가 합계(52,000 *2 +28000 =132000)
        assertThat(savedOrder.getTotalNetAmount()).isEqualTo(132000);

        // NO_TAX 이니까 세금은 0
        assertThat(savedOrder.getTotalTaxAmount()).isEqualTo(0);
        assertThat(savedOrder.getTotalAmount()).isEqualTo(132000);

        List<SalesOrderItem> savedItems = salesOrderItemRepository.findBySalesOrderId(savedOrder.getId());

        assertThat(savedItems).hasSize(2);

        //모든 아이템이 같은 전표에 속하는지
        assertThat(savedItems).allMatch(item ->item.getSalesOrderId().equals(savedOrder.getId()));


        // 첫 번째 아이템 검증
        SalesOrderItem findItem1 = savedItems.stream()
                .filter(item -> item.getProductId().equals(10L))
                .findFirst()
                .orElseThrow();

        assertThat(findItem1.getQuantity()).isEqualTo(2);
        assertThat(findItem1.getUnitPrice()).isEqualTo(52000);
        assertThat(findItem1.getSupplyPrice()).isEqualTo(104000);
        assertThat(findItem1.getTaxPrice()).isEqualTo(0);
        assertThat(findItem1.getTotalPrice()).isEqualTo(104000);
        assertThat(findItem1.getReturnQuantity()).isEqualTo(0);

        // 두 번째 아이템 검증
        SalesOrderItem findItem2 = savedItems.stream()
                .filter(item -> item.getProductId().equals(11L))
                .findFirst()
                .orElseThrow();

        assertThat(findItem2.getQuantity()).isEqualTo(1);
        assertThat(findItem2.getUnitPrice()).isEqualTo(28000);
        assertThat(findItem2.getSupplyPrice()).isEqualTo(28000);
        assertThat(findItem2.getTaxPrice()).isEqualTo(0);
        assertThat(findItem2.getTotalPrice()).isEqualTo(28000);
        assertThat(findItem2.getReturnQuantity()).isEqualTo(0);

    }


    @Test
    @DisplayName("판매 전표 생성 서비스 테스트 - 부가세 포함")
    void creatSalesOrder_addVat() {

        // given
        CreateSalesOrderItemRequest item1 = new CreateSalesOrderItemRequest();
        item1.setProductId(20L);
        item1.setQuantity(2);
        item1.setUnitPrice(50000);

        List<CreateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(item1);

        CreateSalesOrderRequest request = new CreateSalesOrderRequest();
        request.setCustomerId(2L);
        request.setJobSiteId(null);
        request.setPaymentType("CREDIT");
        request.setTaxPolicy("ADD_VAT");
        request.setMemo("부가세 포함 테스트");
        request.setItems(items);

        // when
        SalesOrder savedOrder = salesOrderService.createSalesOrder(request);

        // then
        assertThat(savedOrder.getTotalNetAmount()).isEqualTo(100000);
        assertThat(savedOrder.getTotalTaxAmount()).isEqualTo(10000);
        assertThat(savedOrder.getTotalAmount()).isEqualTo(110000);

        List<SalesOrderItem> savedItems = salesOrderItemRepository.findBySalesOrderId(savedOrder.getId());
        assertThat(savedItems).hasSize(1);

        SalesOrderItem savedItem = savedItems.get(0);

        assertThat(savedItem.getSupplyPrice()).isEqualTo(100000);
        assertThat(savedItem.getTaxPrice()).isEqualTo(10000);
        assertThat(savedItem.getTotalPrice()).isEqualTo(110000);
        assertThat(savedItem.getReturnQuantity()).isEqualTo(0);

    }

}
