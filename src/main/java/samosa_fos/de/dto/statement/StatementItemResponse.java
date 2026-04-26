package samosa_fos.de.dto.statement;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 거래명세표 품목 DTO
public class StatementItemResponse {

    private Integer no;

    private Long productId;
    private String productName;

    private String spec;
    private String unit;

    private Integer quantity;
    private Integer unitPrice;

    private Integer supplyPrice;
    private Integer taxPrice;
    private Integer totalPrice;


}
