package samosa_fos.de.Repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.WebContentGenerator;
import samosa_fos.de.domain.Product;
import samosa_fos.de.repository.ProductRepository;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;



    @Test
    @Transactional
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

    @Test
    @DisplayName("전체 상품 조회 테스트")
    @Transactional
    void findAllProducts() {
        //given
        Product product1 = new Product();
        product1.setCode("P-002");
        product1.setProductName("그린방수 하도");
        product1.setProductNickname("하도");
        product1.setVariant("18L");
        product1.setUnit("말");
        product1.setBrand("삼화");
        product1.setCategory("방수");
        product1.setCostPrice(40000);
        product1.setSalePrice(60000);
        product1.setStockQuantity(5);

        Product product2 = new Product();
        product2.setCode("P-003");
        product2.setProductName("에나멜 흑색");
        product2.setProductNickname("에나멜");
        product2.setVariant("4L");
        product2.setUnit("통");
        product2.setBrand("삼화");
        product2.setCategory("에나멜");
        product2.setCostPrice(20000);
        product2.setSalePrice(28000);
        product2.setStockQuantity(7);

        Product product3 = new Product();
        product2.setCode("P-004");
        product2.setProductName("에나멜 흑색");
        product2.setProductNickname("에나멜");
        product2.setVariant("18L");
        product2.setUnit("말");
        product2.setBrand("삼화");
        product2.setCategory("에나멜");
        product2.setCostPrice(90000);
        product2.setSalePrice(110000);
        product2.setStockQuantity(7);

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);

        //when
        List<Product> products = productRepository.findAll();
        //then
        assertThat(products.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @Transactional
    @DisplayName("상품명으로 검색")
    void findByProductNameContaining (){

        //given
        Product product1 = new Product();
        product1.setCode("P-002");
        product1.setProductName("그린방수 하도");
        product1.setProductNickname("하도");
        product1.setVariant("18L");
        product1.setUnit("말");
        product1.setBrand("삼화");
        product1.setCategory("방수");
        product1.setCostPrice(40000);
        product1.setSalePrice(60000);
        product1.setStockQuantity(5);

        Product product2 = new Product();
        product2.setCode("P-003");
        product2.setProductName("에나멜 흑색");
        product2.setProductNickname("에나멜");
        product2.setVariant("4L");
        product2.setUnit("통");
        product2.setBrand("삼화");
        product2.setCategory("에나멜");
        product2.setCostPrice(20000);
        product2.setSalePrice(28000);
        product2.setStockQuantity(7);

        Product product3 = new Product();
        product3.setCode("P-004");
        product3.setProductName("에나멜 흑색");
        product3.setProductNickname("에나멜");
        product3.setVariant("18L");
        product3.setUnit("말");
        product3.setBrand("삼화");
        product3.setCategory("에나멜");
        product3.setCostPrice(90000);
        product3.setSalePrice(110000);
        product3.setStockQuantity(7);

        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);


        //when
        List<Product> products = productRepository.findByProductNameContaining("에나멜");

        //then
        assertThat(products).isNotNull();
        for (Product p : products) {
            System.out.println(p);
            System.out.println(p.getProductName() + p.getVariant());
        }

    }

}
