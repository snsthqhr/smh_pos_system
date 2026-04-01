package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateSalesOrderRequest {



    // 고객 id (필수)
    private Long customerId;

    // 현장 id (선택)
    // 현장을 지정하지 않는 판매도 가능하므로 null 허용
    private Long jobSiteId;

    // 결제 방식
    // 예: CASH, CARD, TRANSFER, CREDIT
    private String paymentType;

    // 부가세 정책
    // 예: NO_TAX, ADD_VAT
    private String taxPolicy;

    //판매 메모
    private String memo;

    //주문에 담긴 아이템들
    private List<CreateSalesOrderItemRequest> items;

    //판매 가격을 고객가격으로 갱신 할 것인지 아닌지
    // SAVE_PRICE ,ONE_TIME_ONLY
    private String priceApplyPolicy;




}
