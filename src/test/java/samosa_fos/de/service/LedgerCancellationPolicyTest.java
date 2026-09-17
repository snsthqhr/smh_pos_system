package samosa_fos.de.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.LedgerRowDto;
import samosa_fos.de.dto.sales.LedgerSearchRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerCancellationPolicyTest {

    private static final Long CUSTOMER_ID = 20L;
    private static final Long ORDER_ID = 10L;
    private static final LocalDate DATE = LocalDate.of(2026, 9, 17);

    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ArTxRepository arTxRepository;

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(
                salesOrderRepository,
                salesOrderItemRepository,
                paymentRepository,
                productRepository,
                arTxRepository
        );
    }

    @Test
    @DisplayName("외상전표 취소 후 원본·수금·반품·환불·취소 이력이 보존되고 잔액은 0이 된다")
    void creditCancellationKeepsAuditTrailAndBalancesToZero() {
        SalesOrder order = salesOrder("CREDIT", 100000);
        SalesOrderItem item = salesOrderItem(100000);
        Payment payment = payment(100000, true);
        List<ArTx> transactions = List.of(
                arTx("SALE", 100000),
                arTx("PAYMENT", -100000),
                arTx("RETURN", -30000),
                arTx("REFUND_SETTLEMENT", 30000),
                arTx("SALE_CANCEL", -70000),
                arTx("REFUND_SETTLEMENT", 70000)
        );
        arrange(order, item, transactions, List.of(payment));

        List<LedgerRowDto> rows = ledgerService.getLedgerRows(searchRequest());

        assertThat(rows).extracting(LedgerRowDto::getTxType)
                .contains("판매(외상)", "오더합계", "수금", "반품", "환불정산", "판매취소");
        assertThat(rows).allMatch(row -> Boolean.TRUE.equals(row.getCancelledOrder()));
        LedgerRowDto cancellation = rowOfType(rows, "판매취소");
        assertThat(cancellation.getCancelledOrder()).isTrue();
        assertThat(cancellation.getSaleAmount()).isEqualTo(-70000);
        assertThat(cancellation.getArDelta()).isEqualTo(-70000);
        assertThat(rows.get(rows.size() - 1).getBalance()).isZero();
        assertThat(rows.stream().filter(row -> "환불정산".equals(row.getTxType())))
                .extracting(LedgerRowDto::getArDelta)
                .containsExactlyInAnyOrder(30000, 70000);
    }

    @Test
    @DisplayName("즉시결제 전표 취소는 판매와 결제를 반대로 표시하되 미수금은 바꾸지 않는다")
    void immediateCancellationReversesDisplayAmountsWithoutChangingAr() {
        SalesOrder order = salesOrder("CARD", 80000);
        SalesOrderItem item = salesOrderItem(80000);
        Payment inactivePayment = payment(80000, false);
        List<ArTx> transactions = List.of(arTx("SALE_CANCEL", 0));
        arrange(order, item, transactions, List.of());
        when(paymentRepository.findBySalesOrderId(ORDER_ID)).thenReturn(List.of(inactivePayment));

        List<LedgerRowDto> rows = ledgerService.getLedgerRows(searchRequest());

        LedgerRowDto cancellation = rowOfType(rows, "판매취소");
        assertThat(cancellation.getSaleAmount()).isEqualTo(-80000);
        assertThat(cancellation.getPaymentAmount()).isEqualTo(-80000);
        assertThat(cancellation.getArDelta()).isZero();
        assertThat(rows.get(rows.size() - 1).getBalance()).isZero();
        assertThat(rows).noneMatch(row -> "수금".equals(row.getTxType()));
    }

    private void arrange(SalesOrder order,
                         SalesOrderItem item,
                         List<ArTx> transactions,
                         List<Payment> activePayments) {
        when(salesOrderRepository.findByCustomerIdAndSalesDateBetween(CUSTOMER_ID, DATE, DATE))
                .thenReturn(List.of(order));
        when(arTxRepository.findBySalesOrderIdAndTxTypeAndActiveTrue(ORDER_ID, "SALE_CANCEL"))
                .thenReturn(Optional.of(arTx("SALE_CANCEL", 0)));
        when(arTxRepository.findByCustomerIdAndTxDateBeforeAndActiveTrue(CUSTOMER_ID, DATE))
                .thenReturn(List.of());
        when(arTxRepository.findByCustomerIdAndTxDateBetweenAndActiveTrue(CUSTOMER_ID, DATE, DATE))
                .thenReturn(transactions);
        when(paymentRepository.findByCustomerIdAndPaymentDateBetweenAndActiveTrue(CUSTOMER_ID, DATE, DATE))
                .thenReturn(activePayments);
        when(salesOrderItemRepository.findBySalesOrderId(ORDER_ID)).thenReturn(List.of(item));
        when(productRepository.findById(item.getProductId())).thenReturn(Optional.of(product()));
        when(salesOrderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
    }

    private LedgerSearchRequest searchRequest() {
        LedgerSearchRequest request = new LedgerSearchRequest();
        request.setCustomerId(CUSTOMER_ID);
        request.setStartDate(DATE);
        request.setEndDate(DATE);
        request.setShowAll(true);
        return request;
    }

    private SalesOrder salesOrder(String paymentType, int totalAmount) {
        SalesOrder order = new SalesOrder();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setSalesDate(DATE);
        order.setPaymentType(paymentType);
        order.setTotalNetAmount(totalAmount);
        order.setTotalTaxAmount(0);
        order.setTotalAmount(totalAmount);
        order.setActive(false);
        return order;
    }

    private SalesOrderItem salesOrderItem(int totalAmount) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(100L);
        item.setSalesOrderId(ORDER_ID);
        item.setProductId(200L);
        item.setQuantity(1);
        item.setUnitPrice(totalAmount);
        item.setSupplyPrice(totalAmount);
        item.setTaxPrice(0);
        item.setTotalPrice(totalAmount);
        item.setActive(false);
        return item;
    }

    private Product product() {
        Product product = new Product();
        product.setId(200L);
        product.setProductName("테스트 상품");
        product.setUnit("개");
        return product;
    }

    private Payment payment(int amount, boolean active) {
        Payment payment = new Payment();
        payment.setCustomerId(CUSTOMER_ID);
        payment.setSalesOrderId(ORDER_ID);
        payment.setPaymentDate(DATE);
        payment.setAmount(amount);
        payment.setActive(active);
        return payment;
    }

    private ArTx arTx(String type, int amount) {
        ArTx arTx = new ArTx();
        arTx.setCustomerId(CUSTOMER_ID);
        arTx.setSalesOrderId(ORDER_ID);
        arTx.setTxDate(DATE);
        arTx.setTxType(type);
        arTx.setAmount(amount);
        arTx.setActive(true);
        return arTx;
    }

    private LedgerRowDto rowOfType(List<LedgerRowDto> rows, String txType) {
        return rows.stream()
                .filter(row -> txType.equals(row.getTxType()))
                .findFirst()
                .orElseThrow();
    }
}
