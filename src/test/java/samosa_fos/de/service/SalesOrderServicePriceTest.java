package samosa_fos.de.service;


import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.repository.CustomerPriceRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;

@SpringBootTest
@Transactional
public class SalesOrderServicePriceTest {

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CustomerPriceRepository customerPriceRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Test
    @DisplayName("직접 입력 가격이 있으면 직접 입력 가격을 사용한다")
    void createSalesOrder_useRequestUnitPrice() {

        //given
        Product product = new Product();
        product.setCode("P-100");
        product.setProductName("아이생각 수성 내부");
        product.setProductNickname("아이생각");
        product.setVariant("18L");
        product.setUnit("말");
        product.setBrand("삼화");
        product.setCategory("수성");
        product.setCostPrice(35000);
        product.setSalePrice(60000);
        product.setStockQuantity(10);
        //데이터를 저장
        Product savedProduct = productRepository.save(product);


        //when



        //then
    }
}
