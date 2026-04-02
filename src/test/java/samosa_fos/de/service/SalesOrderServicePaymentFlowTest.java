package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@SpringBootTest
@Transactional
public class SalesOrderServicePaymentFlowTest {

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    ArTxRepository arTxRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    @DisplayName("외상 판매면 ArTx가 생성되고 Payment는 생성되지 않는다")
    void createSalesOrder_credit_createsArTxOnly() {

        //given
        Product product = new Product();
        product.setCode("p-201");
        product.setProductName("우레탄 상도");
        product.setVariant("18L");
        product.setUnit("말");
        product.setBrand("삼화");
        product.setCategory("우레탄");
        product.setCostPrice(40000);
        product.setSalePrice(50000);
        product.setStockQuantity(10);
        Product savedProduct = productRepository.save(product);

        CreateSalesOrderItemRequest itemRequest = new CreateSalesOrderItemRequest();
        itemRequest.setProductId(savedProduct.getId());
        itemRequest.setQuantity(2);
        itemRequest.setUnitPrice(null);

        List<CreateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        CreateSalesOrderRequest request = new CreateSalesOrderRequest();
        request.setPaymentType("CREDIT");
        request.setItems(items);
        request.setMemo("외상 판매 테스트");
        request.setCustomerId(1L);
        request.setTaxPolicy("No_TAX");
        request.setJobSiteId(null);
        request.setPriceApplyPolicy("ONE_TIME_ONLY");

        //when
        SalesOrder savedOrder = salesOrderService.createSalesOrder(request);

        //then
        List<ArTx> arTxList = arTxRepository.findBySalesOrderId(savedOrder.getId());
        List<Payment> paymentList = paymentRepository.findBySalesOrderId(savedOrder.getId());

        assertThat(arTxList).hasSize(1);
        assertThat(paymentList).isEmpty();

        ArTx arTx = arTxList.get(0);

        assertThat(arTx.getCustomerId()).isEqualTo(1L);
        assertThat(arTx.getSalesOrderId()).isEqualTo(savedOrder.getId());
        assertThat(arTx.getTxType()).isEqualTo("SALE");
        assertThat(arTx.getAmount()).isEqualTo(100000);

    }

    @Test
    @DisplayName("즉시결제 판매면 Payment가 생성되고 ArTx는 생성되지 않는다")
    void createSalesOrder_cash_createsPaymentOnly() {

        // given
        Product product = new Product();
        product.setCode("P-202");
        product.setProductName("수성 내부 프로");
        product.setProductNickname("아이생각");
        product.setVariant("18L");
        product.setUnit("말");
        product.setBrand("삼화");
        product.setCategory("수성");
        product.setCostPrice(35000);
        product.setSalePrice(52000);
        product.setStockQuantity(10);
        Product savedProduct = productRepository.save(product);

        CreateSalesOrderItemRequest itemRequest = new CreateSalesOrderItemRequest();
        itemRequest.setProductId(savedProduct.getId());
        itemRequest.setQuantity(1);
        itemRequest.setUnitPrice(null); // 자동 가격 사용

        List<CreateSalesOrderItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        CreateSalesOrderRequest request = new CreateSalesOrderRequest();
        request.setCustomerId(2L);
        request.setJobSiteId(null);
        request.setPaymentType("CASH");
        request.setTaxPolicy("NO_TAX");
        request.setMemo("즉시결제 판매 테스트");
        request.setPriceApplyPolicy("ONE_TIME_ONLY");
        request.setItems(items);

        // when
        SalesOrder savedOrder = salesOrderService.createSalesOrder(request);

        // then
        List<Payment> paymentList = paymentRepository.findBySalesOrderId(savedOrder.getId());
        List<ArTx> arTxList = arTxRepository.findBySalesOrderId(savedOrder.getId());

        assertThat(paymentList).hasSize(1);
        assertThat(arTxList).isEmpty();

        Payment payment = paymentList.get(0);

        assertThat(payment.getCustomerId()).isEqualTo(2L);
        assertThat(payment.getSalesOrderId()).isEqualTo(savedOrder.getId());
        assertThat(payment.getPaymentMethod()).isEqualTo("CASH");
        assertThat(payment.getAmount()).isEqualTo(52000);
    }


}
