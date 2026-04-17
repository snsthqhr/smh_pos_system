package samosa_fos.de.dto.sales;

public class UpdateSalesOrderItemRequest {

    private Long salesOrderItemid; // 기존 품목 수정 시 사용(신규는 null)
    private Long productId;
    private Integer quantity;
    private Integer unitPrice;
    
}
