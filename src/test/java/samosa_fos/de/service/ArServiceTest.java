package samosa_fos.de.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.*;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@SpringBootTest
@Transactional
public class ArServiceTest {

    @Autowired
    ArService arService;

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    ReturnService returnService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    ArTxRepository arTxRepository;

    @Test
    @DisplayName("여러 판매/수금/반품과 즉시결제 판매가 섞인 통합 시나리오에서 현재 미수금이 정확히 계산된다")
    void getCurrentArBalance_integratedScenario_success() {

        // given
        Long customerId = 1L;

        Product product1 = new Product();
        product1.setCode("P-5001");
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
        product2.setCode("P-5002");
        product2.setProductName("우레탄 상도");
        product2.setProductNickname("우레탄 상도");
        product2.setVariant("18L");
        product2.setUnit("말");
        product2.setBrand("삼화");
        product2.setCategory("테스트");
        product2.setCostPrice(50000);
        product2.setSalePrice(70000);
        product2.setStockQuantity(100);
        Product savedProduct2 = productRepository.save(product2);

        Product product3 = new Product();
        product3.setCode("P-5003");
        product3.setProductName("에나멜 흑색");
        product3.setProductNickname("에나멜 흑색");
        product3.setVariant("4L");
        product3.setUnit("통");
        product3.setBrand("삼화");
        product3.setCategory("테스트");
        product3.setCostPrice(20000);
        product3.setSalePrice(30000);
        product3.setStockQuantity(100);
        Product savedProduct3 = productRepository.save(product3);

        // 1)외상 판매 1건 = 170,000
        //우레탄 하도 2개(100,000) + 우레탄 상도 1개 (70,000)
        CreateSalesOrderItemRequest creditOrderItem1 = new CreateSalesOrderItemRequest();
        creditOrderItem1.setProductId(savedProduct1.getId());
        creditOrderItem1.setQuantity(2);
        creditOrderItem1.setUnitPrice(50000);

        CreateSalesOrderItemRequest creditOrderItem2 = new CreateSalesOrderItemRequest();
        creditOrderItem2.setProductId(savedProduct2.getId());
        creditOrderItem2.setUnitPrice(70000);
        creditOrderItem2.setQuantity(1);

        List<CreateSalesOrderItemRequest> creditOrder1Items = new ArrayList<>();
        creditOrder1Items.add(creditOrderItem1);
        creditOrder1Items.add(creditOrderItem2);

        CreateSalesOrderRequest creditOrder1Request = new CreateSalesOrderRequest();
        creditOrder1Request.setCustomerId(customerId);
        creditOrder1Request.setJobSiteId(null);
        creditOrder1Request.setMemo("외상 판매 1");
        creditOrder1Request.setPriceApplyPolicy("ONE_TIME_ONLY");
        creditOrder1Request.setItems(creditOrder1Items);
        creditOrder1Request.setPaymentType("CREDIT");

        SalesOrder creditOrder1 = salesOrderService.createSalesOrder(creditOrder1Request);
        //System.out.println(creditOrder1.getTotalAmount());
        ArTx arTx = arTxRepository.findByCustomerId(customerId).get(0);
        System.out.println(arTx.getAmount());
        // 외상 판매 2건 = 120,000
        //에나멜 흑색 4개(120,000)

        CreateSalesOrderItemRequest creditOrder2Item1 = new CreateSalesOrderItemRequest();
        creditOrder2Item1.setProductId(savedProduct3.getId());
        creditOrder2Item1.setQuantity(4);
        creditOrder2Item1.setUnitPrice(30000);

        List<CreateSalesOrderItemRequest> creditOrder2Items = new ArrayList<>();
        creditOrder2Items.add(creditOrder2Item1);

        CreateSalesOrderRequest creditOrder2Request = new CreateSalesOrderRequest();
        creditOrder2Request.setCustomerId(customerId);
        creditOrder2Request.setJobSiteId(null);
        creditOrder2Request.setPaymentType("CREDIT");
        creditOrder2Request.setTaxPolicy("NO_TAX");
        creditOrder2Request.setMemo("외상 판매 2");
        creditOrder2Request.setPriceApplyPolicy("ONE_TIME_ONLY");
        creditOrder2Request.setItems(creditOrder2Items);

        SalesOrder creditOrder2 = salesOrderService.createSalesOrder(creditOrder2Request);

        //즉시결제 판매 1건 = 60000
        //에나멜 흑색 2개(60000)
        //미수금에는 영향 없어야함

        CreateSalesOrderItemRequest cashOrderItem1 = new CreateSalesOrderItemRequest();
        cashOrderItem1.setProductId(savedProduct3.getId());
        cashOrderItem1.setQuantity(2);
        cashOrderItem1.setUnitPrice(30000);

        List<CreateSalesOrderItemRequest> cashOrderItems = new ArrayList<>();
        cashOrderItems.add(cashOrderItem1);

        CreateSalesOrderRequest cashOrder1 = new CreateSalesOrderRequest();
        cashOrder1.setItems(cashOrderItems);
        cashOrder1.setCustomerId(customerId);
        cashOrder1.setMemo("즉시결제 판매");
        cashOrder1.setTaxPolicy("NO_TAX");
        cashOrder1.setPaymentType("CASH");
        cashOrder1.setPriceApplyPolicy("ONE_TIME_ONLY");
        cashOrder1.setJobSiteId(null);

        salesOrderService.createSalesOrder(cashOrder1);


        //----------- 수금------
        // 수금 1 = 50000

        RegisterPaymentRequest paymentRequest1 = new RegisterPaymentRequest();
        paymentRequest1.setCustomerId(customerId);
        paymentRequest1.setSalesOrderId(creditOrder1.getId());
        paymentRequest1.setPaymentDate(LocalDate.now());
        paymentRequest1.setPaymentMethod("TRANSFER");
        paymentRequest1.setMemo("첫 수금");
        paymentRequest1.setAmount(50000);

        paymentService.registerPayment(paymentRequest1);

        //------------- 반품 1----------
        //반품 1 = 50000
        //외상 판매1의 첫 번째 품목(우레탄 하도) 1개 부품
        List<SalesOrderItem> creditOrder1ItemSaved =
                salesOrderItemRepository.findBySalesOrderId(creditOrder1.getId());

        SalesOrderItem firstReturnTarget = creditOrder1ItemSaved.stream()
                .min(Comparator.comparing(SalesOrderItem::getId))
                .orElseThrow();

        ReturnRequest returnRequest1 = new ReturnRequest();
        returnRequest1.setSalesOrderId(firstReturnTarget.getSalesOrderId());
        returnRequest1.setReturnQuantity(1);
        returnRequest1.setSalesOrderItemId(firstReturnTarget.getId());
        returnRequest1.setMemo("첫 반품");

        returnService.addReturnQuantity(returnRequest1);


        //---------------수금 2------
        RegisterPaymentRequest paymentRequest2 = new RegisterPaymentRequest();
        paymentRequest2.setCustomerId(customerId);
        paymentRequest2.setSalesOrderId(creditOrder2.getId());
        paymentRequest2.setPaymentDate(java.time.LocalDate.now());
        paymentRequest2.setAmount(30000);
        paymentRequest2.setPaymentMethod("CASH");
        paymentRequest2.setMemo("두 번째 수금");

        paymentService.registerPayment(paymentRequest2);



        //-------------반품 2 = 30000
        //외상 판매 2의 품목(에나멜 흑색) 1개 반품
        List<SalesOrderItem> creditOrder2ItemsSaved =
                salesOrderItemRepository.findBySalesOrderId(creditOrder2.getId());

        SalesOrderItem secondReturnTarget = creditOrder2ItemsSaved.get(0);

        ReturnRequest returnRequest2 = new ReturnRequest();
        returnRequest2.setSalesOrderItemId(secondReturnTarget.getId());
        returnRequest2.setReturnQuantity(1);
        returnRequest2.setMemo("두 번째 반품");

        returnService.addReturnQuantity(returnRequest2);

        //when
        CustomerArBalanceResponse response =arService.getCurrentArBalanceResponse(customerId);


        //then
        // 외상판매 1: +170,000
        // 외상판매 2: +120,000
        // 즉시결제: +0
        // 수금 1: -50,000
        // 반품 1: -50,000
        // 수금 2: -30,000
        // 반품 2: -30,000
        // 최종 = 130,000
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getCurrentArBalance()).isEqualTo(130000);

        //ArTx 생성 건수도 같이 검증
        List<ArTx> arTxList = arTxRepository.findByCustomerIdAndActiveTrue(customerId);
        assertThat(arTxList).hasSize(6);

        long saleCount = arTxList.stream().filter(tx -> "SALE".equals(tx.getTxType())).count();

        assertThat(saleCount).isEqualTo(2);


    }
}
