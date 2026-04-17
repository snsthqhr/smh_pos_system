package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateSalesOrderRequest {
    private Long salesOrderId;
    private String memo;
    private List<UpdateSalesOrderitemRequest> items;
}
