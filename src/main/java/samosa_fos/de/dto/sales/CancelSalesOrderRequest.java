package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelSalesOrderRequest {

    // 미리보기 이후 금액이 달라졌는지 서버에서 다시 검증할 때 사용한다.
    private Integer expectedRefundAmount;

    private String memo;
}
