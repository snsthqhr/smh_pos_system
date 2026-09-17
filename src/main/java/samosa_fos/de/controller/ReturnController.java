package samosa_fos.de.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.RegisterReturnItemRequest;
import samosa_fos.de.dto.sales.RegisterReturnRequest;
import samosa_fos.de.dto.sales.RegisterReturnResponse;
import samosa_fos.de.dto.sales.ReturnRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;
import samosa_fos.de.service.ReturnService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ArTxRepository arTxRepository;

    public ReturnController(ReturnService returnService,
                            SalesOrderRepository salesOrderRepository,
                            SalesOrderItemRepository salesOrderItemRepository,
                            ArTxRepository arTxRepository) {
        this.returnService = returnService;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.arTxRepository = arTxRepository;
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterReturnResponse registerReturn(@RequestBody RegisterReturnRequest request) {
        validate(request);

        SalesOrder salesOrder = salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(() -> new IllegalArgumentException("판매 전표가 존재하지 않습니다."));

        if (!Boolean.TRUE.equals(salesOrder.getActive())) {
            throw new IllegalArgumentException("취소된 판매 전표는 반품할 수 없습니다.");
        }
        if (!"CREDIT".equals(salesOrder.getPaymentType())) {
            throw new IllegalArgumentException("1차 반품 기능은 외상판매 전표만 처리할 수 있습니다.");
        }

        int returnAmount = 0;
        for (RegisterReturnItemRequest itemRequest : request.getItems()) {
            SalesOrderItem item = salesOrderItemRepository.findById(itemRequest.getSalesOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException("반품할 판매 품목이 존재하지 않습니다."));

            if (!request.getSalesOrderId().equals(item.getSalesOrderId())) {
                throw new IllegalArgumentException("선택한 전표에 포함되지 않은 품목이 있습니다.");
            }
            if (!Boolean.TRUE.equals(item.getActive())) {
                throw new IllegalArgumentException("삭제된 판매 품목은 반품할 수 없습니다.");
            }

            int quantity = itemRequest.getQuantity();
            returnAmount += quantity * nullToZero(item.getUnitPrice());

            ReturnRequest returnRequest = new ReturnRequest();
            returnRequest.setSalesOrderId(request.getSalesOrderId());
            returnRequest.setSalesOrderItemId(itemRequest.getSalesOrderItemId());
            returnRequest.setReturnQuantity(quantity);
            returnRequest.setMemo(request.getMemo());
            returnService.addReturnQuantity(returnRequest);
        }

        int balanceAfterReturn = calculateCurrentArBalance(salesOrder.getCustomerId());
        int refundAmount = balanceAfterReturn < 0 ? Math.abs(balanceAfterReturn) : 0;

        // 반품으로 고객 미수금이 음수가 되면 현장에서 같은 금액을 즉시 돌려주므로
        // 한 트랜잭션 안에서 환불정산 원장까지 자동으로 생성한다.
        if (refundAmount > 0) {
            createRefundSettlement(
                    salesOrder.getCustomerId(),
                    salesOrder.getId(),
                    refundAmount,
                    "POS 반품 자동 환불"
            );
        }

        int balanceAfterProcessing = balanceAfterReturn + refundAmount;

        return new RegisterReturnResponse(
                salesOrder.getId(),
                returnAmount,
                refundAmount,
                balanceAfterProcessing
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<String> handleReturnException(RuntimeException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    private void validate(RegisterReturnRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("반품 정보가 필요합니다.");
        }
        if (request.getSalesOrderId() == null) {
            throw new IllegalArgumentException("반품할 전표를 선택해 주세요.");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("반품할 품목을 1개 이상 입력해 주세요.");
        }

        boolean hasReturnQuantity = request.getItems().stream()
                .anyMatch(item -> item.getQuantity() != null && item.getQuantity() > 0);
        if (!hasReturnQuantity) {
            throw new IllegalArgumentException("반품 수량을 1개 이상 입력해 주세요.");
        }

        List<RegisterReturnItemRequest> invalidItems = request.getItems().stream()
                .filter(item -> item.getSalesOrderItemId() == null || item.getQuantity() == null || item.getQuantity() <= 0)
                .toList();
        if (!invalidItems.isEmpty()) {
            throw new IllegalArgumentException("반품 품목과 수량을 확인해 주세요.");
        }
    }

    private int calculateCurrentArBalance(Long customerId) {
        return arTxRepository.findByCustomerIdAndActiveTrue(customerId).stream()
                .mapToInt(ArTx::getAmount)
                .sum();
    }

    private void createRefundSettlement(Long customerId,
                                        Long salesOrderId,
                                        int refundAmount,
                                        String memo) {
        ArTx settlement = new ArTx();
        settlement.setCustomerId(customerId);
        settlement.setSalesOrderId(salesOrderId);
        settlement.setTxDate(LocalDate.now());
        settlement.setTxType("REFUND_SETTLEMENT");
        settlement.setAmount(refundAmount);
        settlement.setMemo(memo);
        settlement.setActive(true);
        arTxRepository.save(settlement);
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
