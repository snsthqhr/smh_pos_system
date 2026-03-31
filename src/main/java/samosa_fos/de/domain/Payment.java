package samosa_fos.de.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;


    // 어느 고객의 수금인지
    private Long customerId;

    // 어떤 판매 전표와 연결된 수금인지
    // 특정 전표에 바로 받은 돈이면 값 존재
    // 그냥 고객이 한꺼번에 입금한 경우는 null 가능
    private Long salesOrderId;

    // 수금 날짜
    private LocalDate paymentDate;

    // 수금 금액
    private Integer amount;

    // 수금 방식
    // 예: CASH, CARD, TRANSFER
    private String paymentMethod;

    // 수금 메모
    @Column(length = 1000)
    private String memo;

    // 사용 여부
    private Boolean active = true;

    public Payment() {
    }

}
