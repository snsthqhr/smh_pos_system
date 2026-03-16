package samosa_fos.de.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import samosa_fos.de.domain.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product,Long> {

    //상품명 으로 검색
    List<Product> findByProductNameContaining(String productName);

    // 상품 별칭으로 검색
    List<Product> findByProductNicknameContaining(String productNickname);

    //카테고리로 검색
    List<Product> findByCategory(String category);

    //브랜드로 조회
    List<Product> findByBrand(String brand);
}
