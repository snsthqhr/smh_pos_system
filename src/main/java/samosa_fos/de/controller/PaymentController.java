package samosa_fos.de.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.dto.sales.RegisterPaymentRequest;
import samosa_fos.de.dto.sales.RegisterPaymentResponse;
import samosa_fos.de.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterPaymentResponse registerPayment(@RequestBody RegisterPaymentRequest request) {
        Payment payment = paymentService.registerPayment(request);
        return new RegisterPaymentResponse(payment);
    }
}
