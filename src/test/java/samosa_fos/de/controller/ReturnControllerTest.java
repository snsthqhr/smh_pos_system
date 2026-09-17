package samosa_fos.de.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.RegisterReturnItemRequest;
import samosa_fos.de.dto.sales.RegisterReturnRequest;
import samosa_fos.de.dto.sales.RegisterReturnResponse;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;
import samosa_fos.de.service.ReturnService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnControllerTest {

    @Mock
    private ReturnService returnService;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private SalesOrderItemRepository salesOrderItemRepository;
    @Mock
    private ArTxRepository arTxRepository;

    private ReturnController returnController;

    @BeforeEach
    void setUp() {
        returnController = new ReturnController(
                returnService,
                salesOrderRepository,
                salesOrderItemRepository,
                arTxRepository
        );
    }

    @Test
    @DisplayName("반품 후 미수금이 음수면 같은 금액의 환불정산을 자동 생성한다")
    void registerReturn_createsAutomaticRefundSettlement() {
        SalesOrder salesOrder = creditSalesOrder(10L, 20L);
        SalesOrderItem item = salesOrderItem(100L, 10L, 30000);
        RegisterReturnRequest request = returnRequest(10L, 100L, 1);
        ArTx negativeBalance = arTx(-30000);

        when(salesOrderRepository.findById(10L)).thenReturn(Optional.of(salesOrder));
        when(salesOrderItemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(arTxRepository.findByCustomerIdAndActiveTrue(20L)).thenReturn(List.of(negativeBalance));

        RegisterReturnResponse response = returnController.registerReturn(request);

        assertThat(response.getReturnAmount()).isEqualTo(30000);
        assertThat(response.getRefundAmount()).isEqualTo(30000);
        assertThat(response.getBalanceAfterProcessing()).isZero();

        ArgumentCaptor<ArTx> settlementCaptor = ArgumentCaptor.forClass(ArTx.class);
        verify(arTxRepository).save(settlementCaptor.capture());
        ArTx settlement = settlementCaptor.getValue();
        assertThat(settlement.getTxType()).isEqualTo("REFUND_SETTLEMENT");
        assertThat(settlement.getCustomerId()).isEqualTo(20L);
        assertThat(settlement.getSalesOrderId()).isEqualTo(10L);
        assertThat(settlement.getAmount()).isEqualTo(30000);
        assertThat(settlement.getActive()).isTrue();
    }

    @Test
    @DisplayName("반품 후 미수금이 남으면 환불정산을 생성하지 않는다")
    void registerReturn_doesNotCreateSettlementWhenBalanceRemainsPositive() {
        SalesOrder salesOrder = creditSalesOrder(10L, 20L);
        SalesOrderItem item = salesOrderItem(100L, 10L, 30000);
        RegisterReturnRequest request = returnRequest(10L, 100L, 1);
        ArTx positiveBalance = arTx(20000);

        when(salesOrderRepository.findById(10L)).thenReturn(Optional.of(salesOrder));
        when(salesOrderItemRepository.findById(100L)).thenReturn(Optional.of(item));
        when(arTxRepository.findByCustomerIdAndActiveTrue(20L)).thenReturn(List.of(positiveBalance));

        RegisterReturnResponse response = returnController.registerReturn(request);

        assertThat(response.getReturnAmount()).isEqualTo(30000);
        assertThat(response.getRefundAmount()).isZero();
        assertThat(response.getBalanceAfterProcessing()).isEqualTo(20000);
        verify(arTxRepository, never()).save(any(ArTx.class));
    }

    private SalesOrder creditSalesOrder(Long id, Long customerId) {
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setId(id);
        salesOrder.setCustomerId(customerId);
        salesOrder.setPaymentType("CREDIT");
        salesOrder.setActive(true);
        return salesOrder;
    }

    private SalesOrderItem salesOrderItem(Long id, Long salesOrderId, int unitPrice) {
        SalesOrderItem item = new SalesOrderItem();
        item.setId(id);
        item.setSalesOrderId(salesOrderId);
        item.setUnitPrice(unitPrice);
        item.setActive(true);
        return item;
    }

    private RegisterReturnRequest returnRequest(Long salesOrderId, Long itemId, int quantity) {
        RegisterReturnItemRequest itemRequest = new RegisterReturnItemRequest();
        itemRequest.setSalesOrderItemId(itemId);
        itemRequest.setQuantity(quantity);

        RegisterReturnRequest request = new RegisterReturnRequest();
        request.setSalesOrderId(salesOrderId);
        request.setMemo("POS 반품");
        request.setItems(List.of(itemRequest));
        return request;
    }

    private ArTx arTx(int amount) {
        ArTx arTx = new ArTx();
        arTx.setAmount(amount);
        return arTx;
    }
}
