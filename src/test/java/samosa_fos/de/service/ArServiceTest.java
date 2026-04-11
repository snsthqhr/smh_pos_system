package samosa_fos.de.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import samosa_fos.de.domain.Product;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.ArrayList;
import java.util.List;

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
        creditOrder1Request.setItems(cre);



    }
}
