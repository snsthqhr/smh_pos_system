package samosa_fos.de.dto.sales;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class SalesHistoryResponse {

    private Long salesOrderId;
    private LocalDate salesDate;
    private String paymentType;
    private String memo;

    private Integer totalSupplyPrice;
    private Integer totalTxaPrice;
    private Integer totalAmount;

    private List<SalesHistoryItemResponse> items;

}
