package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

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

    public void cancelSalesOrder(Long salesOrderId) {
        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new IllegalArgumentException("판매 전표가 존재하지 않습니다."));

        if (!Boolean.TRUE.equals(salesOrder.getActive())) {
            throw new IllegalStateException("이미 취소된 전표입니다.");
        }

        validateNoReturn(salesOrderId);

        if ("CREDIT".equals(salesOrder.getPaymentType())) {
            cancelCreditSale(salesOrder);
        } else {
            cancelImmediateSale(salesOrder);
        }

        salesOrder.setActive(false);
        salesOrderItemRepository.findBySalesOrderId(salesOrderId)
                .forEach(item -> item.setActive(false));
    }

    private void validateNoReturn(Long salesOrderId) {
        boolean hasReturn = salesOrderItemRepository
                .existsBySalesOrderIdAndReturnQuantityGreaterThan(salesOrderId, 0);

        if (hasReturn) {
            throw new IllegalStateException("반품이 있는 전표는 취소할 수 없습니다.");
        }
    }

    private void cancelCreditSale(SalesOrder salesOrder) {
        // 외상 전표에 수금이 붙어 있으면 취소 시 미수 흐름이 깨지므로 먼저 수금 취소가 필요하다.
        if (paymentRepository.existsBySalesOrderIdAndActiveTrue(salesOrder.getId())) {
            throw new IllegalStateException("수금이 있는 외상 전표는 먼저 수금을 정리해야 취소할 수 있습니다.");
        }

        List<ArTx> activeArTxs = arTxRepository.findBySalesOrderIdAndActiveTrue(salesOrder.getId());
        for (ArTx arTx : activeArTxs) {
            if (!"SALE".equals(arTx.getTxType())) {
                throw new IllegalStateException("판매 외 미수 흐름이 연결된 전표는 취소할 수 없습니다.");
            }
            arTx.setActive(false);
        }
    }

    private void cancelImmediateSale(SalesOrder salesOrder) {
        List<ArTx> activeArTxs = arTxRepository.findBySalesOrderIdAndActiveTrue(salesOrder.getId());
        if (!activeArTxs.isEmpty()) {
            throw new IllegalStateException("미수 흐름이 연결된 즉시결제 전표는 취소할 수 없습니다.");
        }

        List<Payment> activePayments = paymentRepository.findBySalesOrderIdAndActiveTrue(salesOrder.getId());
        if (activePayments.size() > 1) {
            throw new IllegalStateException("즉시결제 전표에 연결된 수금이 여러 건이라 취소할 수 없습니다.");
        }
        activePayments.forEach(payment -> payment.setActive(false));
    }
}
