package samosa_fos.de.Repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.support.WebContentGenerator;
import samosa_fos.de.domain.Product;
import samosa_fos.de.repository.ProductRepository;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;
    @Autowired
    private WebContentGenerator webContentGenerator;


    @Test
    @DisplayName("상품 저장 테스트")
    void saveProduct() {
        //given
        Product product = new Product();
        product.setCode("P-001");
        product.setProductName("아이생각 내부프로");
        product.setProductNickname("아이생각");
        product.setVariant("18L");
        product.setUnit("말");
        product.setBrand("삼화");
        product.setCategory("수성");
        product.setCostPrice(35000);
        product.setSalePrice(52000);
        product.setStockQuantity(10);

        //when
        Product savedProduct = productRepository.save(product);

        //then
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getProductName()).isEqualTo("아이생각 내부프로");
        assertThat(savedProduct.getSalePrice()).isEqualTo(52000);
    }
}
