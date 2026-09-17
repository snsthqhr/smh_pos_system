package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.dto.sales.CancelSalesOrderRequest;
import samosa_fos.de.dto.sales.SalesOrderCancelPreviewResponse;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SalesOrderCancelService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ArTxRepository arTxRepository;

    public SalesOrderCancelService(SalesOrderRepository salesOrderRepository,
                                   SalesOrderItemRepository salesOrderItemRepository,
                                   PaymentRepository paymentRepository,
                                   ArTxRepository arTxRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.arTxRepository = arTxRepository;
    }

    public SalesOrderCancelPreviewResponse previewCancellation(Long salesOrderId) {
        SalesOrder salesOrder = findActiveSalesOrder(salesOrderId);
        return calculatePreview(salesOrder, "취소 전 예상 금액입니다.");
    }

    public SalesOrderCancelPreviewResponse cancelSalesOrder(Long salesOrderId,
                                                             CancelSalesOrderRequest request) {
        SalesOrder salesOrder = salesOrderRepository.findByIdForUpdate(salesOrderId)
                .orElseThrow(() -> new IllegalArgumentException("판매 전표가 존재하지 않습니다."));
        validateActive(salesOrder);

        SalesOrderCancelPreviewResponse preview = calculatePreview(salesOrder, "");
        validateExpectedRefundAmount(request, preview.getRefundAmount());

        String memo = request == null || request.getMemo() == null || request.getMemo().isBlank()
                ? "판매전표 취소"
                : request.getMemo().trim();

        createCancellationArTx(salesOrder, preview.getRemainingSaleAmount(), memo);

        if (isCreditSale(salesOrder) && preview.getRefundAmount() > 0) {
            createCancellationRefundArTx(salesOrder, preview.getRefundAmount());
        }

        if (!isCreditSale(salesOrder)) {
            paymentRepository.findBySalesOrderIdAndActiveTrue(salesOrderId)
                    .forEach(payment -> payment.setActive(false));
        }

        salesOrder.setActive(false);
        salesOrderItemRepository.findBySalesOrderId(salesOrderId)
                .forEach(item -> item.setActive(false));

        String message = preview.getRefundAmount() > 0
                ? "전표가 취소되었습니다. 고객에게 돌려줄 금액: "
                + String.format("%,d", preview.getRefundAmount()) + "원"
                : "전표가 취소되었습니다.";

        return copyWithMessage(preview, message);
    }

    private SalesOrderCancelPreviewResponse calculatePreview(SalesOrder salesOrder, String message) {
        List<ArTx> orderArTxs = arTxRepository.findBySalesOrderIdAndActiveTrue(salesOrder.getId());
        List<Payment> directPayments = paymentRepository.findBySalesOrderIdAndActiveTrue(salesOrder.getId());

        if (orderArTxs.stream().anyMatch(arTx -> "SALE_CANCEL".equals(arTx.getTxType()))) {
            throw new IllegalStateException("이미 판매취소 원장이 생성된 전표입니다.");
        }

        int currentArBalance = calculateCurrentArBalance(salesOrder.getCustomerId());
        int directPaymentAmount = directPayments.stream()
                .mapToInt(payment -> nullToZero(payment.getAmount()))
                .sum();

        if (!isCreditSale(salesOrder)) {
            int saleAmount = nullToZero(salesOrder.getTotalAmount());
            validateImmediatePayment(directPayments, saleAmount);
            return new SalesOrderCancelPreviewResponse(
                    salesOrder.getId(),
                    salesOrder.getCustomerId(),
                    salesOrder.getPaymentType(),
                    saleAmount,
                    directPaymentAmount,
                    0,
                    0,
                    saleAmount,
                    currentArBalance,
                    currentArBalance,
                    directPaymentAmount,
                    currentArBalance,
                    message
            );
        }

        int saleAmount = orderArTxs.stream()
                .filter(arTx -> "SALE".equals(arTx.getTxType()))
                .mapToInt(arTx -> nullToZero(arTx.getAmount()))
                .sum();
        if (saleAmount <= 0) {
            throw new IllegalStateException("외상 판매 원장이 없어 전표를 취소할 수 없습니다.");
        }

        int returnAmount = orderArTxs.stream()
                .filter(arTx -> "RETURN".equals(arTx.getTxType()))
                .mapToInt(arTx -> Math.abs(nullToZero(arTx.getAmount())))
                .sum();
        int previousRefundAmount = orderArTxs.stream()
                .filter(arTx -> "REFUND_SETTLEMENT".equals(arTx.getTxType()))
                .mapToInt(arTx -> nullToZero(arTx.getAmount()))
                .sum();
        int remainingSaleAmount = saleAmount - returnAmount;
        if (remainingSaleAmount < 0) {
            throw new IllegalStateException("반품금액이 판매금액보다 커 전표를 취소할 수 없습니다.");
        }

        int balanceAfterCancellation = currentArBalance - remainingSaleAmount;
        int refundAmount = balanceAfterCancellation < 0 ? Math.abs(balanceAfterCancellation) : 0;
        int balanceAfterProcessing = balanceAfterCancellation + refundAmount;

        return new SalesOrderCancelPreviewResponse(
                salesOrder.getId(),
                salesOrder.getCustomerId(),
                salesOrder.getPaymentType(),
                saleAmount,
                directPaymentAmount,
                returnAmount,
                previousRefundAmount,
                remainingSaleAmount,
                currentArBalance,
                balanceAfterCancellation,
                refundAmount,
                balanceAfterProcessing,
                message
        );
    }

    private void createCancellationArTx(SalesOrder salesOrder, int remainingSaleAmount, String memo) {
        ArTx cancellation = new ArTx();
        cancellation.setCustomerId(salesOrder.getCustomerId());
        cancellation.setSalesOrderId(salesOrder.getId());
        cancellation.setTxDate(LocalDate.now());
        cancellation.setTxType("SALE_CANCEL");
        cancellation.setAmount(isCreditSale(salesOrder) ? -remainingSaleAmount : 0);
        cancellation.setMemo(memo);
        cancellation.setActive(true);
        arTxRepository.save(cancellation);
    }

    private void createCancellationRefundArTx(SalesOrder salesOrder, int refundAmount) {
        ArTx refund = new ArTx();
        refund.setCustomerId(salesOrder.getCustomerId());
        refund.setSalesOrderId(salesOrder.getId());
        refund.setTxDate(LocalDate.now());
        refund.setTxType("REFUND_SETTLEMENT");
        refund.setAmount(refundAmount);
        refund.setMemo("판매전표 취소 현금 환불");
        refund.setActive(true);
        arTxRepository.save(refund);
    }

    private SalesOrder findActiveSalesOrder(Long salesOrderId) {
        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new IllegalArgumentException("판매 전표가 존재하지 않습니다."));
        validateActive(salesOrder);
        return salesOrder;
    }

    private void validateActive(SalesOrder salesOrder) {
        if (!Boolean.TRUE.equals(salesOrder.getActive())) {
            throw new IllegalStateException("이미 취소된 전표입니다.");
        }
    }

    private void validateImmediatePayment(List<Payment> directPayments, int saleAmount) {
        if (directPayments.isEmpty()) {
            throw new IllegalStateException("즉시결제 내역이 없어 전표를 취소할 수 없습니다.");
        }
        if (directPayments.size() > 1) {
            throw new IllegalStateException("즉시결제 내역이 여러 건이라 자동 취소할 수 없습니다.");
        }
        int paymentAmount = nullToZero(directPayments.get(0).getAmount());
        if (paymentAmount <= 0 || paymentAmount != saleAmount) {
            throw new IllegalStateException("즉시결제 금액과 판매금액이 일치하지 않아 자동 취소할 수 없습니다.");
        }
    }

    private void validateExpectedRefundAmount(CancelSalesOrderRequest request, int actualRefundAmount) {
        if (request == null || request.getExpectedRefundAmount() == null) {
            throw new IllegalArgumentException("취소 예상 환불금액을 먼저 확인해 주세요.");
        }
        if (request.getExpectedRefundAmount() != actualRefundAmount) {
            throw new IllegalStateException("미리보기 이후 고객 잔액이 변경되었습니다. 취소 금액을 다시 확인해 주세요.");
        }
    }

    private int calculateCurrentArBalance(Long customerId) {
        return arTxRepository.findByCustomerIdAndActiveTrue(customerId).stream()
                .mapToInt(arTx -> nullToZero(arTx.getAmount()))
                .sum();
    }

    private boolean isCreditSale(SalesOrder salesOrder) {
        return "CREDIT".equals(salesOrder.getPaymentType());
    }

    private SalesOrderCancelPreviewResponse copyWithMessage(SalesOrderCancelPreviewResponse source,
                                                            String message) {
        return new SalesOrderCancelPreviewResponse(
                source.getSalesOrderId(),
                source.getCustomerId(),
                source.getPaymentType(),
                source.getSaleAmount(),
                source.getDirectPaymentAmount(),
                source.getReturnAmount(),
                source.getPreviousRefundAmount(),
                source.getRemainingSaleAmount(),
                source.getCurrentArBalance(),
                source.getBalanceAfterCancellation(),
                source.getRefundAmount(),
                source.getBalanceAfterProcessing(),
                message
        );
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
