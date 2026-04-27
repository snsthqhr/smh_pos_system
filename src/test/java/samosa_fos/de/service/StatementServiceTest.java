package samosa_fos.de.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.domain.SupplierConfig;
import samosa_fos.de.dto.statement.StatementResponse;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;
import samosa_fos.de.repository.SupplierConfigRepository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class StatementServiceTest {

    @Autowired
    StatementService statementService;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplierConfigRepository supplierConfigRepository;

    @Test
    @DisplayName("단일 판매전표 기준 거래명세표 데이터를 정상 생성한다")
    void getStatement_success() {
        // given
        SupplierConfig config = createSupplierConfig();

        Product product1 = createProduct("P-9001", "우레탄 하도", "18L", "말", 50000);
        Product product2 = createProduct("P-9002", "에나멜 흑색", "4L", "통", 30000);

        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setJobSiteId(null);
        order.setPaymentType("CREDIT");
        order.setSalesDate(LocalDate.of(2026, 4, 20));
        order.setTaxPolicy("ADD_VAT");
        order.setMemo("거래명세표 테스트");
        order.setTotalNetAmount(160000);
        order.setTotalTaxAmount(16000);
        order.setTotalAmount(176000);
        order.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(order);

        SalesOrderItem item1 = new SalesOrderItem();
        item1.setSalesOrderId(savedOrder.getId());
        item1.setProductId(product1.getId());
        item1.setQuantity(2);
        item1.setUnitPrice(50000);
        item1.setSupplyPrice(100000);
        item1.setTaxPrice(10000);
        item1.setTotalPrice(110000);
        item1.setReturnQuantity(0);
        item1.setActive(true);
        salesOrderItemRepository.save(item1);

        SalesOrderItem item2 = new SalesOrderItem();
        item2.setSalesOrderId(savedOrder.getId());
        item2.setProductId(product2.getId());
        item2.setQuantity(2);
        item2.setUnitPrice(30000);
        item2.setSupplyPrice(60000);
        item2.setTaxPrice(6000);
        item2.setTotalPrice(66000);
        item2.setReturnQuantity(0);
        item2.setActive(true);
        salesOrderItemRepository.save(item2);

        // when
        StatementResponse response = statementService.getStatement(savedOrder.getId());

        // then
        assertThat(response.getIssueDate()).isEqualTo(LocalDate.now());
        assertThat(response.getCustomerId()).isEqualTo(1L);

        assertThat(response.getTotalSupplyPrice()).isEqualTo(160000);
        assertThat(response.getTotalTaxPrice()).isEqualTo(16000);
        assertThat(response.getTotalAmount()).isEqualTo(176000);

        assertThat(response.getSupplierBusinessNumber()).isEqualTo(config.getBusinessNumber());
        assertThat(response.getSupplierName()).isEqualTo(config.getSupplierName());
        assertThat(response.getSupplierCeoName()).isEqualTo(config.getCeoName());
        assertThat(response.getSupplierAddress()).isEqualTo(config.getAddress());
        assertThat(response.getSupplierBusinessType()).isEqualTo(config.getBusinessType());
        assertThat(response.getSupplierBusinessItem()).isEqualTo(config.getBusinessItem());
        assertThat(response.getSupplierPhone()).isEqualTo(config.getPhone());
        assertThat(response.getSupplierFax()).isEqualTo(config.getFax());
        assertThat(response.getBankAccount()).isEqualTo(config.getBankAccount());
        assertThat(response.getAccountHolder()).isEqualTo(config.getAccountHolder());

        assertThat(response.getItems()).hasSize(2);

        assertThat(response.getItems().get(0).getNo()).isEqualTo(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("우레탄 하도");
        assertThat(response.getItems().get(0).getSpec()).isEqualTo("18L");
        assertThat(response.getItems().get(0).getUnit()).isEqualTo("말");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(0).getUnitPrice()).isEqualTo(50000);
        assertThat(response.getItems().get(0).getSupplyPrice()).isEqualTo(100000);
        assertThat(response.getItems().get(0).getTaxPrice()).isEqualTo(10000);
        assertThat(response.getItems().get(0).getTotalPrice()).isEqualTo(110000);

        assertThat(response.getItems().get(1).getNo()).isEqualTo(2);
        assertThat(response.getItems().get(1).getProductName()).isEqualTo("에나멜 흑색");
        assertThat(response.getItems().get(1).getSpec()).isEqualTo("4L");
        assertThat(response.getItems().get(1).getUnit()).isEqualTo("통");
        assertThat(response.getItems().get(1).getQuantity()).isEqualTo(2);
        assertThat(response.getItems().get(1).getUnitPrice()).isEqualTo(30000);
        assertThat(response.getItems().get(1).getSupplyPrice()).isEqualTo(60000);
        assertThat(response.getItems().get(1).getTaxPrice()).isEqualTo(6000);
        assertThat(response.getItems().get(1).getTotalPrice()).isEqualTo(66000);
    }


    @Test
    @DisplayName("salesOrderId가 null이면 예외가 발생한다")
    void getStatement_fail_whenSalesOrderIdIsNull() {
        // when & then
        assertThatThrownBy(() -> statementService.getStatement(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salesOrderId는 필수입니다.");
    }

    private SupplierConfig createSupplierConfig() {
        SupplierConfig config = new SupplierConfig();
        config.setBusinessNumber("123-45-67890");
        config.setSupplierName("삼화페인트");
        config.setCeoName("홍길동");
        config.setAddress("서울시 강남구 테스트로 123");
        config.setBusinessType("도소매");
        config.setBusinessItem("페인트");
        config.setPhone("02-1234-5678");
        config.setFax("02-1234-9999");
        config.setBankAccount("기업은행 123-456-7890");
        config.setAccountHolder("삼화페인트");
        config.setActive(true);

        return supplierConfigRepository.save(config);
    }

    private Product createProduct(String code,
                                  String productName,
                                  String variant,
                                  String unit,
                                  Integer salePrice) {
        Product product = new Product();
        product.setCode(code);
        product.setProductName(productName);
        product.setProductNickname(productName);
        product.setVariant(variant);
        product.setUnit(unit);
        product.setBrand("삼화");
        product.setCategory("테스트");
        product.setCostPrice(salePrice - 10000);
        product.setSalePrice(salePrice);
        product.setStockQuantity(100);

        return productRepository.save(product);
    }
}