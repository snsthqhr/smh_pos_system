package samosa_fos.de.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.ReturnRequest;
import samosa_fos.de.repository.ArTxRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;

@Service
@Transactional
public class ReturnService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ArTxRepository arTxRepository;

    public ReturnService(SalesOrderRepository salesOrderRepository,
                         SalesOrderItemRepository salesOrderItemRepository,
                         ArTxRepository arTxRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.arTxRepository = arTxRepository;
    }

    // 반품 수량 추가
    public void addReturnQuantity(ReturnRequest request) {

        // 1. 판매 상품 조회
        // 기존
        // SalesOrderItem item = salesOrderItemRepository.findById(request.getSalesOrderId())
        //         .orElseThrow(() -> newIllegalArgumentException("해당 판매 상품이 존재하지 않습니다."));

        // 변경: SalesOrderItem은 salesOrderId가 아니라 salesOrderItemId로 조회해야 함
        SalesOrderItem item = salesOrderItemRepository.findById(request.getSalesOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("해당 판매 상품이 존재하지 않습니다."));

        // 2. 반품 수량 검증
        if (request.getReturnQuantity() == null || request.getReturnQuantity() <= 0) {
            throw new IllegalArgumentException("반품 수량은 1개 이상이어야 합니다.");
        }

        int currentQuantity = item.getQuantity() == null ? 0 : item.getQuantity();
        int currentReturnQuantity = item.getReturnQuantity() == null ? 0 : item.getReturnQuantity();

        int newReturnQuantity = currentReturnQuantity + request.getReturnQuantity();

        if (newReturnQuantity > currentQuantity) {
            throw new IllegalArgumentException("반품 수량이 판매 수량보다 많습니다.");
        }

        // 3. 반품 수량 반영
        item.setReturnQuantity(newReturnQuantity);

        // 4. 연결된 판매 전표 조회
        SalesOrder salesOrder = salesOrderRepository.findById(item.getSalesOrderId())
                .orElseThrow(() -> new IllegalArgumentException("해당 판매 전표가 존재하지 않습니다."));

        // 5. 반품 ArTx 생성
        createReturnArTx(salesOrder, item, request.getReturnQuantity(), request.getMemo());
    }

    // 반품 ArTx 생성
    private ArTx createReturnArTx(SalesOrder salesOrder,
                                  SalesOrderItem item,
                                  int requestReturnQuantity,
                                  String memo) {

        ArTx returnArTx = new ArTx();
        returnArTx.setTxType("RETURN");
        returnArTx.setMemo(memo);
        returnArTx.setActive(true);
        returnArTx.setCustomerId(salesOrder.getCustomerId());
        returnArTx.setSalesOrderId(salesOrder.getId());
        returnArTx.setAmount(calculateReturnAmount(salesOrder, item, requestReturnQuantity));
        returnArTx.setTxDate(LocalDate.now());


        arTxRepository.save(returnArTx);

        return returnArTx;
    }

    // 이번 반품 요청 수량 기준으로 반품 금액 계산
    private int calculateReturnAmount(SalesOrder salesOrder,
                                      SalesOrderItem item,
                                      int requestReturnQuantity) {

        // 기존
        // int returnQuantity = item.getReturnQuantity();
        // int Amount = -(returnQuantity * item.getUnitPrice());

        // 변경: 누적 반품수량이 아니라 "이번 반품수량" 기준으로 계산해야 함
        int supplyAmount = requestReturnQuantity * item.getUnitPrice();
        int taxAmount = "ADD_VAT".equals(salesOrder.getTaxPolicy())
                ? (int) (supplyAmount * 0.1)
                : 0;
        return -(supplyAmount + taxAmount);
    }
}
