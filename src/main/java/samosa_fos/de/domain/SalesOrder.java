package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_id")
    private Long id;//기본키

    private Long customerId;//어느고객에게 판매할 것인지 (Customer 테이블 id)

    //어느 현장에서 사용되는 물건인지(선택값, null이 가능)
    //현장을 지정하지 않는 판매도 존재할 수 있음
    private Long jobSiteId;

    //판매날짜, pos에서 거래가 발생한 실제 날짜
    private LocalDate salesDate;

    // 결제 방식
    // CASH, CARD, TRANSFER, CREDIT(외상)
    private String paymentType;

    //부가세 처리 방식
    // ex: NO_TAX(부가세 없음), ADD_VAT(부가세 추가)
    private String taxPolicy;

    //공급가 합계
    // salesOrderItem들의 공급가 총합
    private Integer totalNetAmount;

    //부가세 금액(10%적용 or not)
    private Integer totalTaxAmount;

    // 최종 결제 금액 (공급가+부가세)
    private Integer totalAmount;

    @Column(length = 1000)
    private String memo;//기타 메모사항 null이 가능함

    //전표 사용 여부
    //삭제 대신 비활성화 처리방식이 맞는듯하다
    private Boolean active = true;

    public SalesOrder() {
    }
}