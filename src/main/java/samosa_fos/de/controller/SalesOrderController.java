package samosa_fos.de.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderResponse;
import samosa_fos.de.service.SalesOrderService;

@RestController
@RequestMapping("/api/sales-orders")
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    public SalesOrderController(SalesOrderService salesOrderService) {
        this.salesOrderService = salesOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSalesOrderResponse createSalesOrder(@RequestBody CreateSalesOrderRequest request) {
        validate(request);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(request);
        return new CreateSalesOrderResponse(salesOrder);
    }

    private void validate(CreateSalesOrderRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "판매 정보가 필요합니다.");
        }
        if (request.getCustomerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "고객은 필수입니다.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "판매 품목은 1개 이상 필요합니다.");
        }
        if (request.getPaymentType() == null || request.getPaymentType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "결제 방식은 필수입니다.");
        }
        if (request.getTaxPolicy() == null || request.getTaxPolicy().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "세금 정책은 필수입니다.");
        }
    }
}
