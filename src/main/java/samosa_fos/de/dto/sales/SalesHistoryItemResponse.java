package samosa_fos.de.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesHistoryItemResponse {

    private String productName;
    private String unit;
    private Integer unitPrice;
    private Integer quantity;
    private Integer returnQuantity;
    private Integer supplyPrice;
    private Integer taxPrice;
    private Integer totalPrice;

}
