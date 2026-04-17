package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateSalesOrderItemRequest {

    private Long salesOrderItemId; // 기존 품목 수정 시 사용(신규는 null)
    private Long productId;
    private Integer quantity;
    private Integer unitPrice;

}
