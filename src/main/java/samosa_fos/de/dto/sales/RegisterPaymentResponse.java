package samosa_fos.de.dto.sales;

import lombok.Getter;
import samosa_fos.de.domain.Payment;

import java.time.LocalDate;

@Getter
public class RegisterPaymentResponse {

    private final Long paymentId;
    private final Long customerId;
    private final Long salesOrderId;
    private final LocalDate paymentDate;
    private final Integer amount;
    private final String paymentMethod;
    private final String memo;

    public RegisterPaymentResponse(Payment payment) {
        this.paymentId = payment.getId();
        this.customerId = payment.getCustomerId();
        this.salesOrderId = payment.getSalesOrderId();
        this.paymentDate = payment.getPaymentDate();
        this.amount = payment.getAmount();
        this.paymentMethod = payment.getPaymentMethod();
        this.memo = payment.getMemo();
    }
}
