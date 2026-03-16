package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(unique = true)
    private String code;

    private String productName;//

    private String productNickname;//별칭

    private String variant;//규격 (18ㅣ/4ㅣ/ 1L / 등)

    private String unit;//가론, 개, 쿼터, 말, 묶음, box등

    private String brand;//브랜드 (삼화, 극동)

    // 판매 카테고리 (에나멜 / 수성 / 우레탄 / 로라 / 실리콘 / 신나 / 붓 / 기타)
    private String category;

    private Integer costPrice;// 입고 단가

    private Integer salePrice;//기본 판매가

   // private String barcode;//바코드를 사용 할지 안할지 미정

    private Integer stockQuantity = 0;//재고

}
