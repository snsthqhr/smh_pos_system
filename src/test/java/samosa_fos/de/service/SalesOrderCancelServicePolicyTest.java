package samosa_fos.de.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.CancelSalesOrderRequest;
import samosa_fos.de.dto.sales.SalesOrderCancelPreviewResponse;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderCancelServicePolicyTest {

    private static final Long ORDER_ID = 10L;
    private static final Long CUSTOMER_ID = 20L;

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ArTxRepository arTxRepository;

    private SalesOrderCancelService service;

    @BeforeEach
    void setUp() {
        service = new SalesOrderCancelService(
                salesOrderRepository,
                salesOrderItemRepository,
                paymentRepository,
                arTxRepository
        );
    }

    @Test
    @DisplayName("미수 상태의 외상전표를 취소하면 남은 미수만 제거하고 환불은 없다")
    void previewCreditSaleWithoutPayment() {
        arrangeCreditPreview(List.of(arTx("SALE", 100000)), List.of(arTx("SALE", 100000)), List.of());

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertPreview(preview, 100000, 0, 0, 100000, 100000, 0, 0);
    }

    @Test
    @DisplayName("전액 수금된 외상전표 취소는 전액 환불한다")
    void previewFullyPaidCreditSale() {
        arrangeCreditPreview(
                List.of(arTx("SALE", 100000), arTx("PAYMENT", -100000)),
                List.of(arTx("SALE", 100000), arTx("PAYMENT", -100000)),
                List.of(payment(100000))
        );

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertPreview(preview, 100000, 100000, 0, 100000, 0, -100000, 100000);
    }

    @Test
    @DisplayName("부분 수금된 외상전표 취소는 받은 금액만 환불한다")
    void previewPartiallyPaidCreditSale() {
        arrangeCreditPreview(
                List.of(arTx("SALE", 100000), arTx("PAYMENT", -60000)),
                List.of(arTx("SALE", 100000), arTx("PAYMENT", -60000)),
                List.of(payment(60000))
        );

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertPreview(preview, 100000, 60000, 0, 100000, 40000, -60000, 60000);
    }

    @Test
    @DisplayName("고객 전체 수금과 다른 전표 미수가 있으면 초과분만 환불한다")
    void previewCustomerWidePaymentWithOtherDebt() {
        arrangeCreditPreview(
                List.of(arTx("SALE", 100000)),
                List.of(arTx("SALE", 100000), arTx("SALE", 50000), arTx("PAYMENT", -100000)),
                List.of()
        );

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertPreview(preview, 100000, 0, 0, 100000, 50000, -50000, 50000);
    }

    @Test
    @DisplayName("다른 전표 미수금이 충분하면 취소 후에도 환불하지 않는다")
    void previewCancellationLeavesOtherDebt() {
        arrangeCreditPreview(
                List.of(arTx("SALE", 100000)),
                List.of(arTx("SALE", 100000), arTx("SALE", 50000)),
                List.of()
        );

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertPreview(preview, 100000, 0, 0, 100000, 150000, 50000, 0);
    }

    @Test
    @DisplayName("기존 반품과 환불이 있으면 아직 취소되지 않은 판매금액만 추가 환불한다")
    void previewSaleWithPreviousReturnAndRefund() {
        List<ArTx> orderTxs = List.of(
                arTx("SALE", 100000),
                arTx("PAYMENT", -100000),
                arTx("RETURN", -30000),
                arTx("REFUND_SETTLEMENT", 30000)
        );
        arrangeCreditPreview(orderTxs, orderTxs, List.of(payment(100000)));

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertThat(preview.getReturnAmount()).isEqualTo(30000);
        assertThat(preview.getPreviousRefundAmount()).isEqualTo(30000);
        assertThat(preview.getRemainingSaleAmount()).isEqualTo(70000);
        assertThat(preview.getRefundAmount()).isEqualTo(70000);
        assertThat(preview.getBalanceAfterProcessing()).isZero();
    }

    @Test
    @DisplayName("전액 반품된 전표는 남은 판매금액과 추가 환불이 없다")
    void previewFullyReturnedSale() {
        List<ArTx> orderTxs = List.of(arTx("SALE", 100000), arTx("RETURN", -100000));
        arrangeCreditPreview(orderTxs, orderTxs, List.of());

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertThat(preview.getRemainingSaleAmount()).isZero();
        assertThat(preview.getRefundAmount()).isZero();
        assertThat(preview.getBalanceAfterProcessing()).isZero();
    }

    @Test
    @DisplayName("즉시결제 전표 취소는 결제금액 전액을 반환하고 미수금은 바꾸지 않는다")
    void previewImmediateSale() {
        SalesOrder order = salesOrder("CARD", 80000);
        arrangePreview(order, List.of(), List.of(arTx("SALE", 50000)), List.of(payment(80000)));

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertThat(preview.getRefundAmount()).isEqualTo(80000);
        assertThat(preview.getCurrentArBalance()).isEqualTo(50000);
        assertThat(preview.getBalanceAfterCancellation()).isEqualTo(50000);
        assertThat(preview.getBalanceAfterProcessing()).isEqualTo(50000);
    }

    @Test
    @DisplayName("외상전표 취소 저장은 SALE_CANCEL과 필요한 환불 원장을 함께 만든다")
    void cancelCreditSaleCreatesReversalAndRefund() {
        SalesOrder order = salesOrder("CREDIT", 100000);
        SalesOrderItem item = salesOrderItem();
        Payment payment = payment(100000);
        List<ArTx> txs = List.of(arTx("SALE", 100000), arTx("PAYMENT", -100000));
        arrangeCancellation(order, txs, txs, List.of(payment), List.of(item));

        SalesOrderCancelPreviewResponse result = service.cancelSalesOrder(ORDER_ID, request(100000));

        ArgumentCaptor<ArTx> captor = ArgumentCaptor.forClass(ArTx.class);
        verify(arTxRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(ArTx::getTxType)
                .containsExactly("SALE_CANCEL", "REFUND_SETTLEMENT");
        assertThat(captor.getAllValues()).extracting(ArTx::getAmount)
                .containsExactly(-100000, 100000);
        assertThat(order.getActive()).isFalse();
        assertThat(item.getActive()).isFalse();
        assertThat(payment.getActive()).isTrue();
        assertThat(result.getBalanceAfterProcessing()).isZero();
    }

    @Test
    @DisplayName("환불 없는 외상전표 취소는 SALE_CANCEL만 만든다")
    void cancelUnpaidCreditSaleCreatesOnlyReversal() {
        SalesOrder order = salesOrder("CREDIT", 100000);
        List<ArTx> txs = List.of(arTx("SALE", 100000));
        arrangeCancellation(order, txs, txs, List.of(), List.of(salesOrderItem()));

        service.cancelSalesOrder(ORDER_ID, request(0));

        ArgumentCaptor<ArTx> captor = ArgumentCaptor.forClass(ArTx.class);
        verify(arTxRepository).save(captor.capture());
        assertThat(captor.getValue().getTxType()).isEqualTo("SALE_CANCEL");
        assertThat(captor.getValue().getAmount()).isEqualTo(-100000);
    }

    @Test
    @DisplayName("즉시결제 취소는 결제를 비활성화하고 미수금 0원 취소 원장만 만든다")
    void cancelImmediateSaleDeactivatesPayment() {
        SalesOrder order = salesOrder("CASH", 80000);
        Payment payment = payment(80000);
        arrangeCancellation(order, List.of(), List.of(arTx("SALE", 50000)), List.of(payment), List.of(salesOrderItem()));

        service.cancelSalesOrder(ORDER_ID, request(80000));

        ArgumentCaptor<ArTx> captor = ArgumentCaptor.forClass(ArTx.class);
        verify(arTxRepository).save(captor.capture());
        assertThat(captor.getValue().getTxType()).isEqualTo("SALE_CANCEL");
        assertThat(captor.getValue().getAmount()).isZero();
        assertThat(payment.getActive()).isFalse();
    }

    @Test
    @DisplayName("미리보기 이후 환불금액이 바뀌면 취소를 차단한다")
    void cancelRejectsStalePreview() {
        SalesOrder order = salesOrder("CREDIT", 100000);
        List<ArTx> txs = List.of(arTx("SALE", 100000), arTx("PAYMENT", -100000));
        when(salesOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(arTxRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(txs);
        when(arTxRepository.findByCustomerIdAndActiveTrue(CUSTOMER_ID)).thenReturn(txs);
        when(paymentRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(List.of(payment(100000)));

        assertThatThrownBy(() -> service.cancelSalesOrder(ORDER_ID, request(50000)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("다시 확인");
        verify(arTxRepository, never()).save(any(ArTx.class));
        assertThat(order.getActive()).isTrue();
    }

    @Test
    @DisplayName("이미 취소된 전표는 다시 취소할 수 없다")
    void rejectAlreadyCancelledOrder() {
        SalesOrder order = salesOrder("CREDIT", 100000);
        order.setActive(false);
        when(salesOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.previewCancellation(ORDER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 취소");
    }

    @Test
    @DisplayName("반품금액이 판매금액보다 크면 취소를 차단한다")
    void rejectReturnAmountGreaterThanSale() {
        arrangeCreditPreview(
                List.of(arTx("SALE", 100000), arTx("RETURN", -110000)),
                List.of(arTx("SALE", 100000), arTx("RETURN", -110000)),
                List.of()
        );

        assertThatThrownBy(() -> service.previewCancellation(ORDER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("반품금액");
    }

    @Test
    @DisplayName("즉시결제 내역이 없거나 여러 건이면 자동 취소를 차단한다")
    void rejectInvalidImmediatePayments() {
        SalesOrder order = salesOrder("CARD", 80000);
        arrangePreview(order, List.of(), List.of(), List.of());
        assertThatThrownBy(() -> service.previewCancellation(ORDER_ID))
                .hasMessageContaining("결제 내역이 없어");
    }

    @Test
    @DisplayName("즉시결제 내역이 여러 건이면 자동 취소를 차단한다")
    void rejectMultipleImmediatePayments() {
        SalesOrder order = salesOrder("CARD", 80000);
        arrangePreview(order, List.of(), List.of(), List.of(payment(30000), payment(50000)));

        assertThatThrownBy(() -> service.previewCancellation(ORDER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("여러 건");
    }

    @Test
    @DisplayName("즉시결제 금액과 판매금액이 다르면 자동 취소를 차단한다")
    void rejectMismatchedImmediatePayment() {
        SalesOrder order = salesOrder("CARD", 80000);
        arrangePreview(order, List.of(), List.of(), List.of(payment(70000)));

        assertThatThrownBy(() -> service.previewCancellation(ORDER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("일치하지 않아");
    }

    @Test
    @DisplayName("취소 저장에는 미리보기에서 확인한 환불금액이 필수다")
    void cancelRequiresExpectedRefundAmount() {
        SalesOrder order = salesOrder("CREDIT", 100000);
        List<ArTx> txs = List.of(arTx("SALE", 100000));
        when(salesOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(arTxRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(txs);
        when(arTxRepository.findByCustomerIdAndActiveTrue(CUSTOMER_ID)).thenReturn(txs);
        when(paymentRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.cancelSalesOrder(ORDER_ID, new CancelSalesOrderRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("먼저 확인");
        verify(arTxRepository, never()).save(any(ArTx.class));
    }

    @Test
    @DisplayName("부가세 포함 판매를 일부 반품한 뒤에는 세금 포함 잔액만 취소한다")
    void previewVatSaleAfterPartialReturn() {
        SalesOrder order = salesOrder("CREDIT", 110000);
        List<ArTx> txs = List.of(arTx("SALE", 110000), arTx("RETURN", -33000));
        arrangePreview(order, txs, txs, List.of());

        SalesOrderCancelPreviewResponse preview = service.previewCancellation(ORDER_ID);

        assertThat(preview.getReturnAmount()).isEqualTo(33000);
        assertThat(preview.getRemainingSaleAmount()).isEqualTo(77000);
        assertThat(preview.getBalanceAfterCancellation()).isZero();
    }

    private void arrangeCreditPreview(List<ArTx> orderTxs,
                                      List<ArTx> customerTxs,
                                      List<Payment> payments) {
        arrangePreview(salesOrder("CREDIT", 100000), orderTxs, customerTxs, payments);
    }

    private void arrangePreview(SalesOrder order,
                                List<ArTx> orderTxs,
                                List<ArTx> customerTxs,
                                List<Payment> payments) {
        when(salesOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(arTxRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(orderTxs);
        when(arTxRepository.findByCustomerIdAndActiveTrue(CUSTOMER_ID)).thenReturn(customerTxs);
        when(paymentRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(payments);
    }

    private void arrangeCancellation(SalesOrder order,
                                     List<ArTx> orderTxs,
                                     List<ArTx> customerTxs,
                                     List<Payment> payments,
                                     List<SalesOrderItem> items) {
        when(salesOrderRepository.findByIdForUpdate(ORDER_ID)).thenReturn(Optional.of(order));
        when(arTxRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(orderTxs);
        when(arTxRepository.findByCustomerIdAndActiveTrue(CUSTOMER_ID)).thenReturn(customerTxs);
        when(paymentRepository.findBySalesOrderIdAndActiveTrue(ORDER_ID)).thenReturn(payments);
        when(salesOrderItemRepository.findBySalesOrderId(ORDER_ID)).thenReturn(items);
    }

    private void assertPreview(SalesOrderCancelPreviewResponse preview,
                               int saleAmount,
                               int directPaymentAmount,
                               int returnAmount,
                               int remainingSaleAmount,
                               int currentBalance,
                               int balanceAfterCancellation,
                               int refundAmount) {
        assertThat(preview.getSaleAmount()).isEqualTo(saleAmount);
        assertThat(preview.getDirectPaymentAmount()).isEqualTo(directPaymentAmount);
        assertThat(preview.getReturnAmount()).isEqualTo(returnAmount);
        assertThat(preview.getRemainingSaleAmount()).isEqualTo(remainingSaleAmount);
        assertThat(preview.getCurrentArBalance()).isEqualTo(currentBalance);
        assertThat(preview.getBalanceAfterCancellation()).isEqualTo(balanceAfterCancellation);
        assertThat(preview.getRefundAmount()).isEqualTo(refundAmount);
        assertThat(preview.getBalanceAfterProcessing()).isEqualTo(balanceAfterCancellation + refundAmount);
    }

    private SalesOrder salesOrder(String paymentType, int totalAmount) {
        SalesOrder order = new SalesOrder();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setPaymentType(paymentType);
        order.setTotalAmount(totalAmount);
        order.setActive(true);
        return order;
    }

    private SalesOrderItem salesOrderItem() {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(100L);
        item.setSalesOrderId(ORDER_ID);
        item.setActive(true);
        return item;
    }

    private Payment payment(int amount) {
        Payment payment = new Payment();
        payment.setCustomerId(CUSTOMER_ID);
        payment.setSalesOrderId(ORDER_ID);
        payment.setAmount(amount);
        payment.setActive(true);
        return payment;
    }

    private ArTx arTx(String type, int amount) {
        ArTx arTx = new ArTx();
        arTx.setCustomerId(CUSTOMER_ID);
        arTx.setSalesOrderId(ORDER_ID);
        arTx.setTxType(type);
        arTx.setAmount(amount);
        arTx.setActive(true);
        return arTx;
    }

    private CancelSalesOrderRequest request(int expectedRefundAmount) {
        CancelSalesOrderRequest request = new CancelSalesOrderRequest();
        request.setExpectedRefundAmount(expectedRefundAmount);
        request.setMemo("테스트 전표 취소");
        return request;
    }
}
