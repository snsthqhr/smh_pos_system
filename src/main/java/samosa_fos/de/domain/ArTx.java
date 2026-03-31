package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@Entity
public class ArTx {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Ar_tx_id")
    private Long id;


    // 어느 고객의 미수금 변동인지
    private Long customerId;

    // 어떤 판매 전표와 연결된 미수금 변동인지
    // 판매/반품은 보통 salesOrderId가 있고
    // 일반 수금은 null일 수도 있음
    private Long salesOrderId;

    // 미수금 변동 날짜
    private LocalDate txDate;

    // 미수금 변동 유형
    // 예: SALE, RETURN, PAYMENT
    private String txType;

    // 미수금 증감 금액
    // 외상 판매: +금액
    // 반품: -금액
    // 수금: -금액
    private Integer amount;

    // 메모
    @Column(length = 1000)
    private String memo;

    // 사용 여부
    private Boolean active = true;

    public ArTx() {
    }

}
