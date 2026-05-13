package samosa_fos.de.dto.sales;

import lombok.Getter;
import samosa_fos.de.domain.SalesOrder;

@Getter
public class CreateSalesOrderResponse {

    private final Long salesOrderId;
    private final Long customerId;
    private final String paymentType;
    private final String taxPolicy;
    private final Integer totalNetAmount;
    private final Integer totalTaxAmount;
    private final Integer totalAmount;

    public CreateSalesOrderResponse(SalesOrder salesOrder) {
        this.salesOrderId = salesOrder.getId();
        this.customerId = salesOrder.getCustomerId();
        this.paymentType = salesOrder.getPaymentType();
        this.taxPolicy = salesOrder.getTaxPolicy();
        this.totalNetAmount = salesOrder.getTotalNetAmount();
        this.totalTaxAmount = salesOrder.getTotalTaxAmount();
        this.totalAmount = salesOrder.getTotalAmount();
    }
}
