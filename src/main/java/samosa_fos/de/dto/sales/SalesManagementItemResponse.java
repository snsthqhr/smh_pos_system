package samosa_fos.de.dto.sales;

import lombok.Getter;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrderItem;

@Getter
public class SalesManagementItemResponse {

    private final Long salesOrderItemId;
    private final Long productId;
    private final String productCode;
    private final String productName;
    private final String unit;
    private final Integer quantity;
    private final Integer returnQuantity;
    private final Integer unitPrice;
    private final Integer supplyPrice;
    private final Integer taxPrice;
    private final Integer totalPrice;

    public SalesManagementItemResponse(SalesOrderItem item, Product product) {
        this.salesOrderItemId = item.getId();
        this.productId = item.getProductId();
        this.productCode = product == null ? null : product.getCode();
        this.productName = product == null ? "상품 조회 불가" : product.getProductName();
        this.unit = product == null ? null : product.getUnit();
        this.quantity = item.getQuantity();
        this.returnQuantity = item.getReturnQuantity();
        this.unitPrice = item.getUnitPrice();
        this.supplyPrice = item.getSupplyPrice();
        this.taxPrice = item.getTaxPrice();
        this.totalPrice = item.getTotalPrice();
    }
}
