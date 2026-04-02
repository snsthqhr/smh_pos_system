package samosa_fos.de.dto.sales;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

//수금 등록을 할때 누가, 얼마나,언제,어떤방식으로를 담는 DTO이다.
@Getter
@Setter
public class RegisterPaymentRequest {

    // 어느 고객의 수금인지
    private Long customerId;

    // 어떤 판매 전표와 연결된 수금인지
    // 특정 전표에 대한 결제면 값 존재
    // 여러 건 묶음 수금이면 null 가능
    private Long salesOrderId;

    // 수금 날짜
    private LocalDate paymentDate;

    // 수금 금액
    private Integer amount;

    // 수금 방식
    // 예: CASH, CARD, TRANSFER
    private String paymentMethod;

    // 수금 메모
    private String memo;

}
