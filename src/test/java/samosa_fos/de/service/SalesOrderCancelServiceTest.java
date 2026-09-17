package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.CancelSalesOrderRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Transactional
class SalesOrderCancelServiceTest {

    @Autowired
    SalesOrderCancelService salesOrderCancelService;

    @Autowired
    SalesOrderRepository salesOrderRepository;

    @Autowired
    SalesOrderItemRepository salesOrderItemRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    ArTxRepository arTxRepository;

    @Test
    @DisplayName("외상 전표를 취소하면 원본 SALE은 보존하고 SALE_CANCEL 역거래를 생성한다")
    void cancelCreditSale_success() {
        SalesOrder order = saveOrder("CREDIT", 100000, 990001L);
        SalesOrderItem item = saveItem(order.getId(), 1, 100000, 0);
        ArTx saleTx = saveArTx(order, "SALE", 100000);

        salesOrderCancelService.cancelSalesOrder(order.getId(), cancelRequest(0));

        assertThat(salesOrderRepository.findById(order.getId()).orElseThrow().getActive()).isFalse();
        assertThat(salesOrderItemRepository.findById(item.getId()).orElseThrow().getActive()).isFalse();
        assertThat(arTxRepository.findById(saleTx.getId()).orElseThrow().getActive()).isTrue();
        assertThat(arTxRepository.findBySalesOrderIdAndActiveTrue(order.getId()).stream()
                .filter(arTx -> "SALE_CANCEL".equals(arTx.getTxType()))
                .mapToInt(ArTx::getAmount)
                .sum()).isEqualTo(-100000);
    }

    @Test
    @DisplayName("수금이 있는 외상 전표는 취소하고 받은 금액만 자동 환불한다")
    void cancelCreditSale_refunds_whenPaymentExists() {
        SalesOrder order = saveOrder("CREDIT", 100000, 990002L);
        saveItem(order.getId(), 1, 100000, 0);
        saveArTx(order, "SALE", 100000);
        saveArTx(order, "PAYMENT", -30000);
        savePayment(order, 30000);

        salesOrderCancelService.cancelSalesOrder(order.getId(), cancelRequest(30000));

        assertThat(arTxRepository.findBySalesOrderIdAndActiveTrue(order.getId()).stream()
                .filter(arTx -> "REFUND_SETTLEMENT".equals(arTx.getTxType()))
                .mapToInt(ArTx::getAmount)
                .sum()).isEqualTo(30000);
    }

    @Test
    @DisplayName("반품이 있는 전표는 남아 있는 판매금액만 취소한다")
    void cancelSale_reversesRemainingAmount_whenReturnExists() {
        SalesOrder order = saveOrder("CREDIT", 100000, 990003L);
        saveItem(order.getId(), 1, 100000, 1);
        saveArTx(order, "SALE", 100000);
        saveArTx(order, "RETURN", -100000);

        salesOrderCancelService.cancelSalesOrder(order.getId(), cancelRequest(0));

        assertThat(arTxRepository.findBySalesOrderIdAndActiveTrue(order.getId()).stream()
                .filter(arTx -> "SALE_CANCEL".equals(arTx.getTxType()))
                .mapToInt(ArTx::getAmount)
                .sum()).isZero();
    }

    @Test
    @DisplayName("즉시결제 전표를 취소하면 연결 Payment가 비활성화된다")
    void cancelImmediateSale_success() {
        SalesOrder order = saveOrder("CARD", 50000, 990004L);
        saveItem(order.getId(), 1, 50000, 0);
        Payment payment = savePayment(order, 50000);

        salesOrderCancelService.cancelSalesOrder(order.getId(), cancelRequest(50000));

        assertThat(salesOrderRepository.findById(order.getId()).orElseThrow().getActive()).isFalse();
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getActive()).isFalse();
    }

    private SalesOrder saveOrder(String paymentType, int totalAmount, Long customerId) {
        SalesOrder order = new SalesOrder();
        order.setCustomerId(customerId);
        order.setSalesDate(LocalDate.now());
        order.setPaymentType(paymentType);
        order.setTaxPolicy("NO_TAX");
        order.setTotalNetAmount(totalAmount);
        order.setTotalTaxAmount(0);
        order.setTotalAmount(totalAmount);
        order.setActive(true);
        return salesOrderRepository.save(order);
    }

    private SalesOrderItem saveItem(Long salesOrderId, int quantity, int unitPrice, int returnQuantity) {
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrderId(salesOrderId);
        item.setProductId(1L);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setSupplyPrice(quantity * unitPrice);
        item.setTaxPrice(0);
        item.setTotalPrice(quantity * unitPrice);
        item.setReturnQuantity(returnQuantity);
        item.setActive(true);
        return salesOrderItemRepository.save(item);
    }

    private ArTx saveArTx(SalesOrder order, String txType, int amount) {
        ArTx arTx = new ArTx();
        arTx.setCustomerId(order.getCustomerId());
        arTx.setSalesOrderId(order.getId());
        arTx.setTxDate(LocalDate.now());
        arTx.setTxType(txType);
        arTx.setAmount(amount);
        arTx.setMemo("테스트");
        arTx.setActive(true);
        return arTxRepository.save(arTx);
    }

    private Payment savePayment(SalesOrder order, int amount) {
        Payment payment = new Payment();
        payment.setCustomerId(order.getCustomerId());
        payment.setSalesOrderId(order.getId());
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(amount);
        payment.setPaymentMethod("CARD");
        payment.setMemo("테스트");
        payment.setActive(true);
        return paymentRepository.save(payment);
    }

    private CancelSalesOrderRequest cancelRequest(int expectedRefundAmount) {
        CancelSalesOrderRequest request = new CancelSalesOrderRequest();
        request.setExpectedRefundAmount(expectedRefundAmount);
        request.setMemo("테스트 전표 취소");
        return request;
    }
}
