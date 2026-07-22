package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.Payment;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.UpdateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.UpdateSalesOrderRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class SalesOrderUpdateService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ArTxRepository arTxRepository;

    public SalesOrderUpdateService(SalesOrderRepository salesOrderRepository,
                                   SalesOrderItemRepository salesOrderItemRepository,
                                   PaymentRepository paymentRepository,
                                   ArTxRepository arTxRepository){

        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.paymentRepository = paymentRepository;
        this.arTxRepository = arTxRepository;

    }

    public void updateSalesOrder(UpdateSalesOrderRequest request) {

        //1. 판매 전표 조회
        SalesOrder salesOrder = salesOrderRepository.findById(request.getSalesOrderId())
                .orElseThrow(()->new IllegalArgumentException("판매 전표가 존재하지 않습니다."));

        // 2026-07-22 정책 변경:
        // 수금이 있는 전표도 조건부로 수정할 수 있게 열었지만,
        // 현재 반품은 returnQuantity 누적 방식이라 원본 판매를 바꾸면 반품 금액 정합성이 깨질 수 있다.
        // 그래서 반품이 있는 전표는 기존처럼 수정 금지한다.
        validateNoReturn(salesOrder.getId());

        // 품목 수량/단가를 먼저 반영해서 수정 후 전표 총액을 계산한다.
        // 이 금액을 기준으로 수금액 초과 여부와 ArTx/Payment 갱신을 판단한다.
        int newTotalAmount = updateItems(salesOrder,request.getItems());

        // 외상 전표는 ArTx(SALE) - ArTx(PAYMENT)가 미수금 기준이다.
        // 수정 후 판매금액이 이미 연결된 수금액보다 작아지면 전표 기준 미수금이 음수가 되므로 저장을 막는다.
        if ("CREDIT".equals(salesOrder.getPaymentType())) {
            validatePaymentCovered(salesOrder.getId(), newTotalAmount);
        }

        // 판매관리 화면에서 수정 가능한 헤더 값: 판매일자, 현장, 메모.
        // 고객명/결제방식/결제금액 분할은 기존 돈 흐름이 꼬일 수 있어 이번 범위에서는 열지 않았다.
        if (request.getSalesDate() != null) {
            salesOrder.setSalesDate(request.getSalesDate());
        }
        salesOrder.setJobSiteId(request.getJobSiteId());
        salesOrder.setMemo(request.getMemo());

        // 결제 방식에 따라 돈 흐름의 기준 데이터가 다르다.
        // 외상은 ArTx(SALE)를 수정하고, 즉시결제는 연결 Payment를 수정한다.
        updateMoneyFlow(salesOrder,newTotalAmount);
    }


    /**
     * 수정 가능 여부 검증
     *
     */
    private void validateNoReturn(Long salesOrderId){
        boolean hasReturn = salesOrderItemRepository
                .existsBySalesOrderIdAndReturnQuantityGreaterThan(salesOrderId,0);

        if(hasReturn) {
            throw new IllegalStateException("이미 반품이 존재하여 수정할 수 없습니다.");
        }

    }

    private void validatePaymentCovered(Long salesOrderId, int newTotalAmount) {
        // 특정 전표에 직접 연결된 활성 수금만 비교한다.
        // 고객 전체 수금(salesOrderId=null)은 고객 단위 수금이라 특정 전표 수정 제한에는 직접 사용하지 않는다.
        int paidAmount = paymentRepository.findBySalesOrderIdAndActiveTrue(salesOrderId).stream()
                .mapToInt(payment -> payment.getAmount() == null ? 0 : payment.getAmount())
                .sum();

        if (newTotalAmount < paidAmount) {
            throw new IllegalStateException("수정 후 판매금액이 이미 수금된 금액보다 작아 수정할 수 없습니다.");
        }
    }

    private List<Payment> findActivePayments(Long salesOrderId) {
        return paymentRepository.findBySalesOrderIdAndActiveTrue(salesOrderId);
    }

    private int updateItems (SalesOrder salesOrder, List<UpdateSalesOrderItemRequest> itemRequests){

        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("판매 품목은 1개 이상 필요합니다.");
        }

        int totalNet = 0;
        int totalTax = 0;
        int totalAmount = 0;
        // 이번 수정 요청에 포함된 품목 id를 모아두고,
        // 요청에서 빠진 기존 품목은 아래에서 active=false 처리한다.
        Set<Long> keptItemIds = new java.util.HashSet<>();

        for(UpdateSalesOrderItemRequest req : itemRequests) {
            if (req.getProductId() == null) {
                throw new IllegalArgumentException("상품은 필수입니다.");
            }
            if (req.getQuantity() == null || req.getQuantity() <= 0) {
                throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
            }
            if (req.getUnitPrice() == null || req.getUnitPrice() < 0) {
                throw new IllegalArgumentException("단가는 0 이상이어야 합니다.");
            }

            SalesOrderItem item;

            //기존 품목 수정
            if (req.getSalesOrderItemId() != null) {
                item = salesOrderItemRepository.findById(req.getSalesOrderItemId())
                        .orElseThrow(() -> new IllegalArgumentException("품목이 존재하지 않습니다."));
                if (!salesOrder.getId().equals(item.getSalesOrderId())) {
                    throw new IllegalArgumentException("해당 전표의 품목이 아닙니다.");
                }
            }else {
                // 신규 품목 추가 전에 없던 품목이 추가 된경우
                item = new SalesOrderItem();
                item.setSalesOrderId(salesOrder.getId());
                item.setReturnQuantity(0);
                item.setActive(true);
            }
            item.setProductId(req.getProductId());
            item.setQuantity(req.getQuantity());
            item.setUnitPrice(req.getUnitPrice());
            //공급가액 갱신
            int supplyPrice = req.getQuantity() * req.getUnitPrice();

            int taxPrice = 0;
            if("ADD_VAT".equals(salesOrder.getTaxPolicy())) {
                taxPrice = (int) (supplyPrice * 0.1);
            }

            int totalPrice = supplyPrice + taxPrice;

            item.setSupplyPrice(supplyPrice);
            item.setTaxPrice(taxPrice);
            item.setTotalPrice(totalPrice);

            totalNet += supplyPrice;
            totalTax += taxPrice;
            totalAmount += totalPrice;

            SalesOrderItem savedItem = salesOrderItemRepository.save(item);
            keptItemIds.add(savedItem.getId());
        }

        // 수정 요청에 포함되지 않은 기존 품목은 실제 삭제하지 않고 비활성화한다.
        // 이렇게 해야 과거 전표/이력 추적 가능성을 남겨둘 수 있다.
        salesOrderItemRepository.findBySalesOrderId(salesOrder.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .filter(item -> item.getId() != null)
                .filter(item -> !keptItemIds.contains(item.getId()))
                .forEach(item -> item.setActive(false));

        //SalesOrder 전체 금액 갱신 업데이트
        salesOrder.setTotalNetAmount(totalNet);
        salesOrder.setTotalTaxAmount(totalTax);
        salesOrder.setTotalAmount(totalAmount);

        return totalAmount;
    }


    private void updateMoneyFlow(SalesOrder salesOrder, int totalAmount) {
        if ("CREDIT".equals(salesOrder.getPaymentType())) {
            updateArTx(salesOrder, totalAmount);
            return;
        }

        updateImmediatePayment(salesOrder, totalAmount);
    }

    private void updateArTx(SalesOrder salesOrder, int totalAmount){

        // 외상 판매는 미수금 기준 데이터가 ArTx(SALE)이므로
        // 전표 수정 후 총액과 판매일자를 ArTx에도 맞춰준다.
        ArTx arTx = arTxRepository
                .findBySalesOrderIdAndTxTypeAndActiveTrue(salesOrder.getId(),"SALE")
                .orElseThrow(() -> new IllegalArgumentException("SALE ArTx가 존재하지 않습니다."));

        arTx.setTxDate(salesOrder.getSalesDate());
        arTx.setAmount(totalAmount);
    }

    private void updateImmediatePayment(SalesOrder salesOrder, int totalAmount) {
        // 즉시결제 판매는 생성 시 ArTx(SALE)을 만들지 않고 Payment만 만든다.
        // 그래서 카드/현금/통장입금 전표 수정 시에는 연결된 즉시결제 Payment 금액을 같이 맞춘다.
        List<Payment> activePayments = findActivePayments(salesOrder.getId());
        if (activePayments.isEmpty()) {
            return;
        }
        if (activePayments.size() > 1) {
            throw new IllegalStateException("즉시결제 전표에 연결된 수금이 여러 건이라 자동 수정할 수 없습니다.");
        }

        Payment payment = activePayments.get(0);
        payment.setPaymentDate(salesOrder.getSalesDate());
        payment.setPaymentMethod(salesOrder.getPaymentType());
        payment.setAmount(totalAmount);
    }

}
