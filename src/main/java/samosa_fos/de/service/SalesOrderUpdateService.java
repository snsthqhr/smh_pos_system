package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.UpdateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.UpdateSalesOrderRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.PaymentRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.List;

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

        //2. 수정 가능 여부 검증
        validateUpdatable(salesOrder.getId());
        //3. 기존  금액
        int oldTotalAmount = salesOrder.getTotalAmount() == null? 0 : salesOrder.getTotalAmount();
        //4. 품목 수정 및 금액 재계산,금액 갱신
        int newTotalAmount = updateItems(salesOrder,request.getItems());

        //5.salesOrder 메모 업데이트
        salesOrder.setMemo(request.getMemo());

        //6.ArTx(SALE)수정
        updateArTx(salesOrder.getId(),newTotalAmount);
    }



    private void validateUpdatable(Long salesOrderId){
        boolean hasPayment = paymentRepository.
                existsBySalesOrderIdAndActiveTrue(salesOrderId);
        boolean hasReturn = salesOrderItemRepository
                .existsBySalesOrderIdAndReturnQuantityGreaterThan(salesOrderId,0);

        if (hasPayment) {
            throw new IllegalStateException("이미 수금이 존재하여 수정할 수 없습니다.");

        }
        if(hasReturn) {
            throw new IllegalStateException("이미 반품이 존재하여 수정할 수 없습니다.");
        }

    }

    private int updateItems (SalesOrder salesOrder, List<UpdateSalesOrderItemRequest> itemRequests){

        int totalNet = 0;
        int totalTax = 0;
        int totalAmount = 0;

        for(UpdateSalesOrderItemRequest req : itemRequests) {

            SalesOrderItem item;

            //기존 품목 수정
            if (req.getSalesOrderItemId() != null) {
                item = salesOrderItemRepository.findById(req.getSalesOrderItemId())
                        .orElseThrow(() -> new IllegalArgumentException("품목이 존재하지 않습니다."));
            }else {
                // 신규 품목 추가 전에 없던 품목이 추가 된경우
                item = new SalesOrderItem();
                item.setSalesOrderId(salesOrder.getId());
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


        }

        //SalesOrder 전체 금액 갱신 업데이트
        salesOrder.setTotalNetAmount(totalNet);
        salesOrder.setTotalTaxAmount(totalTax);
        salesOrder.setTotalAmount(totalAmount);

        return totalAmount;
    }



}
