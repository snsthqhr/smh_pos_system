package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.*;
import samosa_fos.de.dto.sales.UpdateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.UpdateSalesOrderRequest;
import samosa_fos.de.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
public class SalesOrderUpdateServiceTest {
    @Autowired
    SalesOrderUpdateService salesOrderUpdateService;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ArTxRepository arTxRepository;

    @Test
    @DisplayName("수금/반품이 없으면 판매 품목 수량과 단가를 수정할 수 있고, SalesOrder와 ArTx도 함께 수정된다")
    void updateSalesOrder_success() {
        // given
        Long customerId = 1L;

        Product product1 = new Product();
        product1.setCode("P-7001");
        product1.setProductName("우레탄 하도");
        product1.setProductNickname("우레탄 하도");
        product1.setVariant("18L");
        product1.setUnit("말");
        product1.setBrand("삼화");
        product1.setCategory("테스트");
        product1.setCostPrice(40000);
        product1.setSalePrice(50000);
        product1.setStockQuantity(100);
        Product savedProduct1 = productRepository.save(product1);

        Product product2 = new Product();
        product2.setCode("P-7002");
        product2.setProductName("에나멜 흑색");
        product2.setProductNickname("에나멜 흑색");
        product2.setVariant("4L");
        product2.setUnit("통");
        product2.setBrand("삼화");
        product2.setCategory("테스트");
        product2.setCostPrice(20000);
        product2.setSalePrice(30000);
        product2.setStockQuantity(100);
        Product savedProduct2 = productRepository.save(product2);

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(customerId);
        salesOrder.setJobSiteId(null);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setMemo("원본 판매");
        salesOrder.setTotalNetAmount(130000);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(130000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item1 = new SalesOrderItem();
        item1.setSalesOrderId(savedOrder.getId());
        item1.setProductId(savedProduct1.getId());
        item1.setQuantity(2); // 100000
        item1.setUnitPrice(50000);
        item1.setSupplyPrice(100000);
        item1.setTaxPrice(0);
        item1.setTotalPrice(100000);
        item1.setReturnQuantity(0);
        item1.setActive(true);
        SalesOrderItem savedItem1 = salesOrderItemRepository.save(item1);

        SalesOrderItem item2 = new SalesOrderItem();
        item2.setSalesOrderId(savedOrder.getId());
        item2.setProductId(savedProduct2.getId());
        item2.setQuantity(1); // 30000
        item2.setUnitPrice(30000);
        item2.setSupplyPrice(30000);
        item2.setTaxPrice(0);
        item2.setTotalPrice(30000);
        item2.setReturnQuantity(0);
        item2.setActive(true);
        SalesOrderItem savedItem2 = salesOrderItemRepository.save(item2);

        ArTx saleTx = new ArTx();
        saleTx.setCustomerId(customerId);
        saleTx.setSalesOrderId(savedOrder.getId());
        saleTx.setTxDate(LocalDate.now());
        saleTx.setTxType("SALE");
        saleTx.setAmount(130000);
        saleTx.setMemo("판매");
        saleTx.setActive(true);
        arTxRepository.save(saleTx);

        // 수정 요청
        // item1: 2개 * 50000 -> 3개 * 50000 = 150000
        // item2: 1개 * 30000 -> 2개 * 40000 = 80000
        // 총합 = 230000
        UpdateSalesOrderItemRequest req1 = new UpdateSalesOrderItemRequest();
        req1.setSalesOrderItemId(savedItem1.getId());
        req1.setProductId(savedProduct1.getId());
        req1.setQuantity(3);
        req1.setUnitPrice(50000);

        UpdateSalesOrderItemRequest req2 = new UpdateSalesOrderItemRequest();
        req2.setSalesOrderItemId(savedItem2.getId());
        req2.setProductId(savedProduct2.getId());
        req2.setQuantity(2);
        req2.setUnitPrice(40000);

        List<UpdateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(req1);
        items.add(req2);

        UpdateSalesOrderRequest request = new UpdateSalesOrderRequest();
        request.setSalesOrderId(savedOrder.getId());
        request.setMemo("수정된 판매");
        request.setItems(items);

        // when
        salesOrderUpdateService.updateSalesOrder(request);

        // then
        SalesOrder updatedOrder = salesOrderRepository.findById(savedOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getMemo()).isEqualTo("수정된 판매");
        assertThat(updatedOrder.getTotalNetAmount()).isEqualTo(230000);
        assertThat(updatedOrder.getTotalTaxAmount()).isEqualTo(0);
        assertThat(updatedOrder.getTotalAmount()).isEqualTo(230000);

        SalesOrderItem updatedItem1 = salesOrderItemRepository.findById(savedItem1.getId()).orElseThrow();
        assertThat(updatedItem1.getQuantity()).isEqualTo(3);
        assertThat(updatedItem1.getUnitPrice()).isEqualTo(50000);
        assertThat(updatedItem1.getSupplyPrice()).isEqualTo(150000);
        assertThat(updatedItem1.getTotalPrice()).isEqualTo(150000);

        SalesOrderItem updatedItem2 = salesOrderItemRepository.findById(savedItem2.getId()).orElseThrow();
        assertThat(updatedItem2.getQuantity()).isEqualTo(2);
        assertThat(updatedItem2.getUnitPrice()).isEqualTo(40000);
        assertThat(updatedItem2.getSupplyPrice()).isEqualTo(80000);
        assertThat(updatedItem2.getTotalPrice()).isEqualTo(80000);

        ArTx updatedSaleTx = arTxRepository
                .findBySalesOrderIdAndTxTypeAndActiveTrue(savedOrder.getId(), "SALE")
                .orElseThrow();

        assertThat(updatedSaleTx.getAmount()).isEqualTo(230000);
    }

    @Test
    @DisplayName("이미 수금이 존재하면 판매내역을 수정할 수 없다")
    void updateSalesOrder_fail_whenPaymentExists() {
        // given
        Long customerId = 2L;

        Product product = new Product();
        product.setCode("P-7101");
        product.setProductName("우레탄 상도");
        product.setProductNickname("우레탄 상도");
        product.setVariant("18L");
        product.setUnit("말");
        product.setBrand("삼화");
        product.setCategory("테스트");
        product.setCostPrice(50000);
        product.setSalePrice(70000);
        product.setStockQuantity(100);
        Product savedProduct = productRepository.save(product);

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(customerId);
        salesOrder.setJobSiteId(null);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setMemo("수금 존재 전표");
        salesOrder.setTotalNetAmount(70000);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(70000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(savedProduct.getId());
        item.setQuantity(1);
        item.setUnitPrice(70000);
        item.setSupplyPrice(70000);
        item.setTaxPrice(0);
        item.setTotalPrice(70000);
        item.setReturnQuantity(0);
        item.setActive(true);
        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        Payment payment = new Payment();
        payment.setCustomerId(customerId);
        payment.setSalesOrderId(savedOrder.getId());
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(30000);
        payment.setPaymentMethod("TRANSFER");
        payment.setMemo("일부 수금");
        payment.setActive(true);
        paymentRepository.save(payment);

        UpdateSalesOrderItemRequest itemRequest = new UpdateSalesOrderItemRequest();
        itemRequest.setSalesOrderItemId(savedItem.getId());
        itemRequest.setProductId(savedProduct.getId());
        itemRequest.setQuantity(2);
        itemRequest.setUnitPrice(70000);

        List<UpdateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        UpdateSalesOrderRequest request = new UpdateSalesOrderRequest();
        request.setSalesOrderId(savedOrder.getId());
        request.setMemo("수정 시도");
        request.setItems(items);

        // when & then
        assertThatThrownBy(() -> salesOrderUpdateService.updateSalesOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 수금이 존재하여 수정할 수 없습니다.");
    }

    @Test
    @DisplayName("이미 반품이 존재하면 판매내역을 수정할 수 없다")
    void updateSalesOrder_fail_whenReturnExists() {
        // given
        Long customerId = 3L;

        Product product = new Product();
        product.setCode("P-7201");
        product.setProductName("에나멜 백색");
        product.setProductNickname("에나멜 백색");
        product.setVariant("4L");
        product.setUnit("통");
        product.setBrand("삼화");
        product.setCategory("테스트");
        product.setCostPrice(20000);
        product.setSalePrice(30000);
        product.setStockQuantity(100);
        Product savedProduct = productRepository.save(product);

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(customerId);
        salesOrder.setJobSiteId(null);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setSalesDate(LocalDate.now());
        salesOrder.setTaxPolicy("NO_TAX");
        salesOrder.setMemo("반품 존재 전표");
        salesOrder.setTotalNetAmount(60000);
        salesOrder.setTotalTaxAmount(0);
        salesOrder.setTotalAmount(60000);
        salesOrder.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(salesOrder);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(savedProduct.getId());
        item.setQuantity(2);
        item.setUnitPrice(30000);
        item.setSupplyPrice(60000);
        item.setTaxPrice(0);
        item.setTotalPrice(60000);
        item.setReturnQuantity(1); // 이미 반품 존재
        item.setActive(true);
        SalesOrderItem savedItem = salesOrderItemRepository.save(item);

        UpdateSalesOrderItemRequest itemRequest = new UpdateSalesOrderItemRequest();
        itemRequest.setSalesOrderItemId(savedItem.getId());
        itemRequest.setProductId(savedProduct.getId());
        itemRequest.setQuantity(3);
        itemRequest.setUnitPrice(30000);

        List<UpdateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        UpdateSalesOrderRequest request = new UpdateSalesOrderRequest();
        request.setSalesOrderId(savedOrder.getId());
        request.setMemo("수정 시도");
        request.setItems(items);

        // when & then
        assertThatThrownBy(() -> salesOrderUpdateService.updateSalesOrder(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 반품이 존재하여 수정할 수 없습니다.");
    }

}
