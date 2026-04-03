package samosa_fos.de.dto.sales;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LedgerRowDto {

    // 정렬과 표시 기준 날짜
    private LocalDate txDate;

    // 거래구분
    // 예: 판매(외상), 판매(즉시결제), 반품, 수금, 오더합계
    private String txType;

    // 같은 주문끼리 묶어보기 위한 값
    private Long salesOrderId;

    // 고객 정보
    private Long customerId;
    private String customerName;

    // 품목 정보
    private String productName;
    private String unit;

    // 단가 / 수량
    private Integer unitPrice;
    private Integer quantity;

    // 금액 정보
    private Integer supplyPrice;
    private Integer taxPrice;
    private Integer saleAmount;     // 판매금액
    private Integer paymentAmount;  // 수금금액

    // 미수금 증감
    private Integer arDelta;

    // 누적잔액
    private Integer balance;

    // 합계 행인지 여부
    private Boolean summaryRow = false;

    // 메모
    private String memo;

}
