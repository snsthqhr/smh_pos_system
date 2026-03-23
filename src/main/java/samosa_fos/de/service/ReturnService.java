package samosa_fos.de.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.List;

import static org.h2.mvstore.DataUtils.newIllegalArgumentException;

@Service
@Transactional
public class ReturnService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;

    public ReturnService(SalesOrderRepository salesOrderRepository,
                         SalesOrderItemRepository salesOrderItemRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
    }

    //반품 수량 추가
    public void addReturnQuantity(Long salesOrderItemId, Integer returnQunatity){
        //1. 판매 상품 조회
        SalesOrderItem item = salesOrderItemRepository.findById(salesOrderItemId)
                .orElseThrow(()-> newIllegalArgumentException("해당 판매 상품이 존재하지 않습니다."));

        //2. 반품 수량 검증
        if(returnQunatity == null || returnQunatity <= 0) {
            throw new IllegalArgumentException("반품 수량은 1개 이상이어야 합니다.");
        }

        int currentQuantity = item.getQuantity();
        int currentReturnQuantity = item.getReturnQuantity();

        int newReturnQuantity = currentReturnQuantity + returnQunatity;

        if(newReturnQuantity > item.getQuantity()) {
            throw new IllegalArgumentException("반품 수량이 판매 수량보다 많습니다.");

        }

        //3. 반품 수량 반영
        item.setReturnQuantity(newReturnQuantity);


        // 4. 해당 전표 다시 계산
        Long salesOrderId = item.getSalesOrderId();

        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 판매 전표가 존재하지 않습니다."));

        List<SalesOrderItem> items = salesOrderItemRepository.findBySalesOrderId(salesOrderId);

        int totalNetAmount = 0;
        int totalTaxAmount = 0;
        int totalAmount = 0;

        for (SalesOrderItem orderItem : items) {
            int quantity = orderItem.getQuantity() == null ? 0 : orderItem.getQuantity();
            int returned = orderItem.getReturnQuantity() == null ? 0 : orderItem.getReturnQuantity();
            int remainingQuantity = quantity - returned;

            int unitPrice = orderItem.getUnitPrice() == null ? 0 : orderItem.getUnitPrice();

            int supplyPrice = remainingQuantity * unitPrice;
            int taxPrice = 0;

            if ("ADD_VAT".equals(salesOrder.getTaxPolicy())) {
                taxPrice = (int) (supplyPrice * 0.1);
            }

            int totalPrice = supplyPrice + taxPrice;

            // 반품 후 현재 금액 상태를 item에도 반영
            orderItem.setSupplyPrice(supplyPrice);
            orderItem.setTaxPrice(taxPrice);
            orderItem.setTotalPrice(totalPrice);

            totalNetAmount += supplyPrice;
            totalTaxAmount += taxPrice;
            totalAmount += totalPrice;
        }

    }



}
