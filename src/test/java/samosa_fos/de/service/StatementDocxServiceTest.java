package samosa_fos.de.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
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
    @DisplayName("품목이 10개 이상인 거래명세표 DOCX 파일을 정상 생성한다")
    void createStatementDocx_success_withMoreThan10Items() throws Exception {
        // given
        createSupplierConfig();

        SalesOrder order = new SalesOrder();
        order.setCustomerId(1L);
        order.setJobSiteId(null);
        order.setPaymentType("CREDIT");
        order.setSalesDate(LocalDate.of(2026, 4, 27));
        order.setTaxPolicy("ADD_VAT");
        order.setMemo("DOCX 10개 이상 품목 테스트");
        order.setActive(true);

        SalesOrder savedOrder = salesOrderRepository.save(order);

        int totalSupply = 0;
        int totalTax = 0;
        int totalAmount = 0;

        for (int i = 1; i <= 12; i++) {
            Product product = createProduct(
                    "P-DOCX-" + i,
                    "테스트 상품 " + i,
                    "18L",
                    "말",
                    10000 * i
            );

            int quantity = i;
            int unitPrice = 10000 * i;
            int supplyPrice = quantity * unitPrice;
            int taxPrice = (int) (supplyPrice * 0.1);
            int itemTotalPrice = supplyPrice + taxPrice;

            SalesOrderItem item = new SalesOrderItem();
            item.setSalesOrderId(savedOrder.getId());
            item.setProductId(product.getId());
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setSupplyPrice(supplyPrice);
            item.setTaxPrice(taxPrice);
            item.setTotalPrice(itemTotalPrice);
            item.setReturnQuantity(0);
            item.setActive(true);

            salesOrderItemRepository.save(item);

            totalSupply += supplyPrice;
            totalTax += taxPrice;
            totalAmount += itemTotalPrice;
        }

        savedOrder.setTotalNetAmount(totalSupply);
        savedOrder.setTotalTaxAmount(totalTax);
        savedOrder.setTotalAmount(totalAmount);

        // when
        byte[] docxBytes = statementDocxService.createStatementDocx(savedOrder.getId());

        // then
        assertThat(docxBytes).isNotNull();
        assertThat(docxBytes.length).isGreaterThan(0);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {

            String tableText = document.getTables()
                    .stream()
                    .flatMap(table -> table.getRows().stream())
                    .flatMap(row -> row.getTableCells().stream())
                    .map(XWPFTableCell::getText)
                    .reduce("", (a, b) -> a + " " + b);

            assertThat(tableText).contains("테스트 상품 1");
            assertThat(tableText).contains("테스트 상품 10");
            assertThat(tableText).contains("테스트 상품 12");

            assertThat(tableText).contains("합계");

        }
    }


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
        config.setCeoName("신동우");
        config.setAddress("test_address");
        config.setBusinessType("도소매");
        config.setBusinessItem("페인트");
        config.setPhone("010-2539-6373");
        config.setFax("031-756-8945");
        config.setBankAccount("우리은행 1002-962-791749");
        config.setAccountHolder("황경애");
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