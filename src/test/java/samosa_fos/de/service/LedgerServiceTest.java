package samosa_fos.de.service;


import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Product;
import samosa_fos.de.dto.sales.*;
import samosa_fos.de.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
class LedgerServiceTest {


    @Autowired
    LedgerService ledgerService;

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ArTxRepository arTxRepository;


    @Test
    @DisplayName("서비스 기반: 외상 판매 + 수금 후 원장에 시작 잔액, 오더합계, 수금이 정확히 반영되는지")
    void getLedgerRows_withServices_creditSaleAndPayment() {
        //given
        Long customerId = 1L;

        Product product1 = createProduct("P-401", "우레탄 하도", "말", 50000);
        Product product2 = createProduct("P-402","우레탄 상도", "말", 70000);

        ArTx openingTx = new ArTx();
        openingTx.setCustomerId(customerId);
        openingTx.setSalesOrderId(999L);
        openingTx.setActive(true);
        openingTx.setMemo("전기이월");
        openingTx.setTxDate(LocalDate.of(2026,4,1));
        openingTx.setAmount(300000);
        arTxRepository.save(openingTx);

        CreateSalesOrderItemRequest itemRequest1 = new CreateSalesOrderItemRequest();
        itemRequest1.setUnitPrice(50000);
        itemRequest1.setQuantity(2);
        itemRequest1.setProductId(product1.getId());

        CreateSalesOrderItemRequest itemRequest2 = new CreateSalesOrderItemRequest();
        itemRequest2.setUnitPrice(70000);
        itemRequest2.setQuantity(1);
        itemRequest2.setProductId(product2.getId());

        List<CreateSalesOrderItemRequest> items = new ArrayList<>();

        items.add(itemRequest1);
        items.add(itemRequest2);

        CreateSalesOrderRequest salesOrderRequest = new CreateSalesOrderRequest();
        salesOrderRequest.setItems(items);
        salesOrderRequest.setJobSiteId(null);
        salesOrderRequest.setPaymentType("CREDIT");
        salesOrderRequest.setMemo("외상 판매 테스트");
        salesOrderRequest.setTaxPolicy("NO_TAX");
        salesOrderRequest.setCustomerId(customerId);
        salesOrderRequest.setPriceApplyPolicy("ONE_TIME_ONLY");

        salesOrderService.createSalesOrder(salesOrderRequest);

        RegisterPaymentRequest paymentRequest = new RegisterPaymentRequest();
        paymentRequest.setCustomerId(customerId);
        paymentRequest.setSalesOrderId(null);
        paymentRequest.setPaymentDate(LocalDate.now());
        paymentRequest.setAmount(50000);
        paymentRequest.setPaymentMethod("TRANSFER");
        paymentRequest.setMemo("일부 수금");

        paymentService.registerPayment(paymentRequest);

        LedgerSearchRequest ledgerRequest = new LedgerSearchRequest();
        ledgerRequest.setCustomerId(customerId);
        ledgerRequest.setStartDate(LocalDate.now());
        ledgerRequest.setEndDate(LocalDate.now());
        ledgerRequest.setShowAll(true);

        //when

        List<LedgerRowDto> rows = ledgerService.getLedgerRows(ledgerRequest);

        //then

        LedgerRowDto firstItemRow = rows.get(0);
        LedgerRowDto secondItemRow = rows.get(1);
        LedgerRowDto summaryRow = rows.get(2);
        LedgerRowDto paymentRow = rows.get(3);

        // 품목행1
        assertThat(firstItemRow.getTxType()).isEqualTo("판매(외상)");
        assertThat(firstItemRow.getProductName()).isEqualTo("우레탄 하도");
        assertThat(firstItemRow.getQuantity()).isEqualTo(2);
        assertThat(firstItemRow.getSaleAmount()).isEqualTo(100000);
        assertThat(firstItemRow.getArDelta()).isEqualTo(0);
        assertThat(firstItemRow.getBalance()).isEqualTo(300000);

        // 품목행2
        assertThat(secondItemRow.getTxType()).isEqualTo("판매(외상)");
        assertThat(secondItemRow.getProductName()).isEqualTo("우레탄 상도");
        assertThat(secondItemRow.getQuantity()).isEqualTo(1);
        assertThat(secondItemRow.getSaleAmount()).isEqualTo(70000);
        assertThat(secondItemRow.getArDelta()).isEqualTo(0);
        assertThat(secondItemRow.getBalance()).isEqualTo(300000);

        // 오더합계행
        assertThat(summaryRow.getTxType()).isEqualTo("오더합계");
        assertThat(summaryRow.getSummaryRow()).isTrue();
        assertThat(summaryRow.getQuantity()).isEqualTo(3);
        assertThat(summaryRow.getSaleAmount()).isEqualTo(170000);
        assertThat(summaryRow.getArDelta()).isEqualTo(170000);
        assertThat(summaryRow.getBalance()).isEqualTo(470000);

        // 수금행
        assertThat(paymentRow.getTxType()).isEqualTo("수금");
        assertThat(paymentRow.getPaymentAmount()).isEqualTo(50000);
        assertThat(paymentRow.getArDelta()).isEqualTo(-50000);
        assertThat(paymentRow.getBalance()).isEqualTo(420000);

    }

    @Test
    @DisplayName("서비스 기반: 즉시결제 판매는 원장에 표시되지만 잔액은 안변함")
    void getLedgerRows_withServices_immediateSaleDoesNotChangeBalance() {


        // given
        Long customerId = 2L;

        Product product = createProduct("P-403", "에나멜 흑색", "통", 28000);

        // 시작 잔액
        ArTx openingTx = new ArTx();
        openingTx.setCustomerId(customerId);
        openingTx.setSalesOrderId(888L);
        openingTx.setTxDate(LocalDate.of(2026, 4, 1));
        openingTx.setTxType("SALE");
        openingTx.setAmount(100000);
        openingTx.setMemo("전기이월");
        openingTx.setActive(true);
        arTxRepository.save(openingTx);

        // 즉시결제 판매 생성
        CreateSalesOrderItemRequest itemRequest = new CreateSalesOrderItemRequest();
        itemRequest.setProductId(product.getId());
        itemRequest.setQuantity(2);
        itemRequest.setUnitPrice(28000);

        List<CreateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        CreateSalesOrderRequest salesRequest = new CreateSalesOrderRequest();
        salesRequest.setCustomerId(customerId);
        salesRequest.setJobSiteId(null);
        salesRequest.setPaymentType("CASH");
        salesRequest.setTaxPolicy("NO_TAX");
        salesRequest.setMemo("즉시결제 판매");
        salesRequest.setPriceApplyPolicy("ONE_TIME_ONLY");
        salesRequest.setItems(items);

        salesOrderService.createSalesOrder(salesRequest);

        LedgerSearchRequest ledgerRequest = new LedgerSearchRequest();
        ledgerRequest.setCustomerId(customerId);
        ledgerRequest.setStartDate(LocalDate.now());
        ledgerRequest.setEndDate(LocalDate.now());
        ledgerRequest.setShowAll(true);


        // when
        List<LedgerRowDto> rows = ledgerService.getLedgerRows(ledgerRequest);

        // then
        // 품목행 1개 + 오더합계 1개 + 수금행 1개(즉시결제 Payment 표시용)


        LedgerRowDto itemRow = rows.get(0);
        LedgerRowDto summaryRow = rows.get(1);
        LedgerRowDto paymentRow = rows.get(2);

        assertThat(itemRow.getTxType()).isEqualTo("판매(즉시결제)");
        assertThat(itemRow.getPaymentAmount()).isEqualTo(56000);
        assertThat(itemRow.getArDelta()).isEqualTo(0);
        assertThat(itemRow.getBalance()).isEqualTo(100000);

        assertThat(summaryRow.getTxType()).isEqualTo("오더합계");
        assertThat(summaryRow.getPaymentAmount()).isEqualTo(56000);
        assertThat(summaryRow.getArDelta()).isEqualTo(0);
        assertThat(summaryRow.getBalance()).isEqualTo(100000);

        assertThat(paymentRow.getTxType()).isEqualTo("수금");
        assertThat(paymentRow.getPaymentAmount()).isEqualTo(56000);
        assertThat(paymentRow.getArDelta()).isEqualTo(0); // PAYMENT ArTx 없음
        assertThat(paymentRow.getBalance()).isEqualTo(100000);

    }








    private Product createProduct(String code, String productName, String unit, Integer salePrice) {
        Product product = new Product();
        product.setCode(code);
        product.setProductName(productName);
        product.setProductNickname(productName);
        product.setVariant("18L");
        product.setUnit(unit);
        product.setBrand("삼화");
        product.setCategory("테스트");
        product.setCostPrice(salePrice - 10000);
        product.setSalePrice(salePrice);
        product.setStockQuantity(100);
        return productRepository.save(product);
    }




}
