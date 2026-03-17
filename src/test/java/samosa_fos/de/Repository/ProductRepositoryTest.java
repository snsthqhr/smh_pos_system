package samosa_fos.de.Repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.Product;
import samosa_fos.de.repository.ProductRepository;

@SpringBootTest
public class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;


    @Test
    @DisplayName("상품 저장 테스트")
    void saveProduct() {

        Product product = new Product();
        Product.setCode("P-001");
        product.setProductName("아이생각 내부프로");


    }
}
