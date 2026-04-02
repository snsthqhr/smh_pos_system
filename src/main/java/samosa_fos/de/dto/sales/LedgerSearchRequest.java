package samosa_fos.de.dto.sales;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LedgerSearchRequest {

    // 어떤 고객의 원장을 조회할지
    private Long customerId ;


    // 조회 시작일
    private LocalDate startDate;

    // 조회 종료일
    private LocalDate endDate;

    //전체 표시 여부
    private Boolean showAll = true;

    // 판매 표시 여부
    private Boolean showSales = true;

    // 외상 판매만 표시 여부
    private Boolean showCreditSales = true;

    // 즉시 결제 판매 표시 여부
    private Boolean showImmediateSales = true;

    // 반품 표시 여부
    private Boolean showReturns = true ;

    // 수금 표시 여부
    private Boolean showPayments = true;

}
