package samosa_fos.de.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.domain.SupplierConfig;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;
import samosa_fos.de.repository.SupplierConfigRepository;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StatementDocxServiceTest {

    @Autowired
    StatementDocxService statementDocxService;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    SupplierConfigRepository supplierConfigRepository;

    @Test
    @DisplayName("거래명세표 DOCX 파일을 정상 생성한다")
    void createStatementDocx_success() throws Exception {
        // given
        createSupplierConfig();

        Product product = createProduct("P-10001", "우레탄 하도", "18L", "말", 50000);

        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setJobSiteId(null);
        order.setPaymentType("CREDIT");
        order.setSalesDate(LocalDate.of(2026, 4, 27));
        order.setTaxPolicy("ADD_VAT");
        order.setMemo("DOCX 테스트");
        order.setTotalNetAmount(100000);
        order.setTotalTaxAmount(10000);
        order.setTotalAmount(110000);
        order.setActive(true);
        SalesOrder savedOrder = salesOrderRepository.save(order);

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(savedOrder.getId());
        item.setProductId(product.getId());
        item.setQuantity(2);
        item.setUnitPrice(50000);
        item.setSupplyPrice(100000);
        item.setTaxPrice(10000);
        item.setTotalPrice(110000);
        item.setReturnQuantity(0);
        item.setActive(true);
        salesOrderItemRepository.save(item);

        // when
        byte[] docxBytes = statementDocxService.createStatementDocx(savedOrder.getId());

        // then
        assertThat(docxBytes).isNotNull();
        assertThat(docxBytes.length).isGreaterThan(0);

        // 실제 Word 문서로 열 수 있는지 확인
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            String allText = document.getParagraphs()
                    .stream()
                    .map(p -> p.getText())
                    .reduce("", (a, b) -> a + " " + b);

            assertThat(allText).contains("거 래 명 세 표");

            String tableText = document.getTables()
                    .stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(row -> row.getTableCells().stream())
                    .map(cell -> cell.getText())
                    .reduce("", (a, b) -> a + " " + b);

            assertThat(tableText).contains("우레탄 하도");
            assertThat(tableText).contains("삼화페인트");
            assertThat(tableText).contains("100,000");
            assertThat(tableText).contains("10,000");
        }
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