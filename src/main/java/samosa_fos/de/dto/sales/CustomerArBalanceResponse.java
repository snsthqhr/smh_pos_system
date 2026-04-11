package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerArBalanceResponse {

    //고객 아이디
    private Long customerId;

    // 고객 미수금 조회
    private int currentArBalance;

}
