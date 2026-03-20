package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSalesOrderItemRequest {

    private Long productId;
    private Long salesOrderId;
    private Integer quantity;
    private Integer unitPrice;

}
