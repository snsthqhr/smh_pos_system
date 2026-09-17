package samosa_fos.de.dto.sales;

import lombok.Getter;

@Getter
public class SalesOrderCancelPreviewResponse {

    private final Long salesOrderId;
    private final Long customerId;
    private final String paymentType;
    private final int saleAmount;
    private final int directPaymentAmount;
    private final int returnAmount;
    private final int previousRefundAmount;
    private final int remainingSaleAmount;
    private final int currentArBalance;
    private final int balanceAfterCancellation;
    private final int refundAmount;
    private final int balanceAfterProcessing;
    private final String message;

    public SalesOrderCancelPreviewResponse(Long salesOrderId,
                                           Long customerId,
                                           String paymentType,
                                           int saleAmount,
                                           int directPaymentAmount,
                                           int returnAmount,
                                           int previousRefundAmount,
                                           int remainingSaleAmount,
                                           int currentArBalance,
                                           int balanceAfterCancellation,
                                           int refundAmount,
                                           int balanceAfterProcessing,
                                           String message) {
        this.salesOrderId = salesOrderId;
        this.customerId = customerId;
        this.paymentType = paymentType;
        this.saleAmount = saleAmount;
        this.directPaymentAmount = directPaymentAmount;
        this.returnAmount = returnAmount;
        this.previousRefundAmount = previousRefundAmount;
        this.remainingSaleAmount = remainingSaleAmount;
        this.currentArBalance = currentArBalance;
        this.balanceAfterCancellation = balanceAfterCancellation;
        this.refundAmount = refundAmount;
        this.balanceAfterProcessing = balanceAfterProcessing;
        this.message = message;
    }
}
