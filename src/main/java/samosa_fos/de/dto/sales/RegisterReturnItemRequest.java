package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterReturnItemRequest {

    private Long salesOrderItemId;

    private Integer quantity;
}
