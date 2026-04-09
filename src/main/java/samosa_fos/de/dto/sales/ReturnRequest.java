package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReturnRequest {


    //반품할 제품이 포함된 주문아이디
    private Long salesOrderId;

    //반품 할 제품의 아이디
    private Long salesOrderItemId;

    //메모
    private String memo;

    //반품 할 수량
    private Integer returnQuantity;


}
