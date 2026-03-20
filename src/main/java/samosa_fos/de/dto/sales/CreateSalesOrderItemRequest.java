package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSalesOrderItemRequest {

    //제품의 아이디
    private Long productId;

    //제품의 수량
    private Integer quantity;

    //제품의 가격
    private Integer unitPrice;

}
