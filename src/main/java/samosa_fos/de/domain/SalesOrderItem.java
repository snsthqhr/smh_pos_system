package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SalesOrderItem {

    // 판매 상품 PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_item_id")
    private Long id;

    // 어떤 판매 전표에 속하는지
    private Long salesOrderId;

    // 어떤 상품인지
    private Long productId;

    // 판매 수량
    private Integer quantity;

    // 판매 당시 단가
    // 고객가격 / 현장가격 / 직접수정 가격
    private Integer unitPrice;

    // 공급가 (수량 × 단가)
    private Integer supplyPrice;

    // 부가세 금액
    private Integer taxPrice;

    // 총 금액 (공급가 + 부가세)
    private Integer totalPrice;

    // 사용 여부 (삭제 대신 비활성화)
    private Boolean active = true;

    // JPA 기본 생성자
    protected SalesOrderItem() {
    }
}