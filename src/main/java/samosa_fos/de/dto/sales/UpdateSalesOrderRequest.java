package samosa_fos.de.dto.sales;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateSalesOrderRequest {
    private Long salesOrderId;
    // 판매관리 화면에서 헤더 수정 대상으로 연 판매일자.
    private LocalDate salesDate;
    // 현장은 기존 고객의 현장 검색 결과에서 선택하거나 null로 비울 수 있다.
    private Long jobSiteId;
    private String memo;
    private List<UpdateSalesOrderItemRequest> items;
}
