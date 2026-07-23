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
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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
    @DisplayName("외상 전표를 취소하면 전표, 품목, ArTx SALE이 비활성화된다")
    void cancelCreditSale_success() {
        SalesOrder order = saveOrder("CREDIT", 100000);
        SalesOrderItem item = saveItem(order.getId(), 1, 100000, 0);
        ArTx saleTx = saveArTx(order.getId(), "SALE", 100000);

        salesOrderCancelService.cancelSalesOrder(order.getId());

        assertThat(salesOrderRepository.findById(order.getId()).orElseThrow().getActive()).isFalse();
        assertThat(salesOrderItemRepository.findById(item.getId()).orElseThrow().getActive()).isFalse();
        assertThat(arTxRepository.findById(saleTx.getId()).orElseThrow().getActive()).isFalse();
    }

    @Test
    @DisplayName("수금이 있는 외상 전표는 취소할 수 없다")
    void cancelCreditSale_fail_whenPaymentExists() {
        SalesOrder order = saveOrder("CREDIT", 100000);
        saveItem(order.getId(), 1, 100000, 0);
        saveArTx(order.getId(), "SALE", 100000);
        savePayment(order.getId(), 30000);

        assertThatThrownBy(() -> salesOrderCancelService.cancelSalesOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("수금");
    }

    @Test
    @DisplayName("반품이 있는 전표는 취소할 수 없다")
    void cancelSale_fail_whenReturnExists() {
        SalesOrder order = saveOrder("CREDIT", 100000);
        saveItem(order.getId(), 1, 100000, 1);
        saveArTx(order.getId(), "SALE", 100000);

        assertThatThrownBy(() -> salesOrderCancelService.cancelSalesOrder(order.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("반품");
    }

    @Test
    @DisplayName("즉시결제 전표를 취소하면 연결 Payment가 비활성화된다")
    void cancelImmediateSale_success() {
        SalesOrder order = saveOrder("CARD", 50000);
        saveItem(order.getId(), 1, 50000, 0);
        Payment payment = savePayment(order.getId(), 50000);

        salesOrderCancelService.cancelSalesOrder(order.getId());

        assertThat(salesOrderRepository.findById(order.getId()).orElseThrow().getActive()).isFalse();
        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getActive()).isFalse();
    }

    private SalesOrder saveOrder(String paymentType, int totalAmount) {
        SalesOrder order = new SalesOrder();
        order.setCustomerId(999L);
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

    private ArTx saveArTx(Long salesOrderId, String txType, int amount) {
        ArTx arTx = new ArTx();
        arTx.setCustomerId(999L);
        arTx.setSalesOrderId(salesOrderId);
        arTx.setTxDate(LocalDate.now());
        arTx.setTxType(txType);
        arTx.setAmount(amount);
        arTx.setMemo("테스트");
        arTx.setActive(true);
        return arTxRepository.save(arTx);
    }

    private Payment savePayment(Long salesOrderId, int amount) {
        Payment payment = new Payment();
        payment.setCustomerId(999L);
        payment.setSalesOrderId(salesOrderId);
        payment.setPaymentDate(LocalDate.now());
        payment.setAmount(amount);
        payment.setPaymentMethod("CARD");
        payment.setMemo("테스트");
        payment.setActive(true);
        return paymentRepository.save(payment);
    }
}
