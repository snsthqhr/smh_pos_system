package samosa_fos.de.dto.sales;

import lombok.Getter;

@Getter
public class RegisterReturnResponse {

    private final Long salesOrderId;
    private final int returnAmount;
    private final int refundAmount;
    private final int balanceAfterProcessing;
    private final String message;

    public RegisterReturnResponse(Long salesOrderId,
                                  int returnAmount,
                                  int refundAmount,
                                  int balanceAfterProcessing) {
        this.salesOrderId = salesOrderId;
        this.returnAmount = returnAmount;
        this.refundAmount = refundAmount;
        this.balanceAfterProcessing = balanceAfterProcessing;
        this.message = refundAmount > 0
                ? "반품과 현금 환불이 함께 저장되었습니다. 환불금액: " + String.format("%,d", refundAmount) + "원"
                : "반품이 저장되었습니다.";
    }
}
