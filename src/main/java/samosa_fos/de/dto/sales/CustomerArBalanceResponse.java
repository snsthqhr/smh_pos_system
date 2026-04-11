package samosa_fos.de.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerArBalanceResponse {

    //고객 아이디
    private Long customerId;

    // 고객 미수금 금액
    private Integer currentArBalance;

}
