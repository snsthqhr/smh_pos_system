package samosa_fos.de.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;

@Service
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository salesOrderItemRepository){
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
    }

    //기본베이스는 dto를 활용하여 주입받는다
    public SalesOrder CreateSalesOrder(CreateSalesOrderRequest request){
        //판매전표 헤더 생성
        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setCustomerId(request.getCustomerId());
        salesOrder.setJobSiteId(request.getJobSiteId());
        salesOrder.setActive(Boolean.TRUE);
        salesOrder.setMemo(request.getMemo());
        salesOrder.setTaxPolicy(request.getTaxPolicy());
        salesOrder.setPaymentType(request.getPaymentType());


        //dto로 받을까 그냥 서비스에서 받을까 고민중임.일단 서비스로 구현
        salesOrder.setSalesDate(LocalDate.now());

        //일단 총금액 0원으로 초기화
        salesOrder.setTotalAmount(0);

        int totalOrderPrice = 0;


        //id값 생성을 위해서 salesorderId는 데이터베이스에 저장될때 generatevalue를 통해 id 값이 생성된다.
        SalesOrder savedSalesOrder = salesOrderRepository.save(salesOrder);

        for(CreateSalesOrderItemRequest itemRequest : request.getItems()) {

            SalesOrderItem item  = new SalesOrderItem();
            //genrate된 키값을 이 아팀에 salesOrderId로 설정
            item.setSalesOrderId(savedSalesOrder.getId());

            //제품의 아이디
            item.setProductId(itemRequest.getProductId());

            //제품의 수량
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(item.getUnitPrice());

            //제품의 반품 내역
            //제품의 판매 시점에서는 반품이 0인게 당연한것
            item.setReturnQuantity(0);

            //제품의 공급가
            int supplyPrice = itemRequest.getUnitPrice()* item.getQuantity();
            item.setSupplyPrice(supplyPrice);


            //세금계산
            // 지금은 간단히 taxPolicy가 NO_TAX면 0, 아니면 10%처리한다
            int taxPrice = 0;
            if("ADD_VAT".equals(request.getTaxPolicy())){
                taxPrice = (int) (supplyPrice * 0.1);
            }
            item.setTaxPrice(taxPrice);//만약 ADD_VAT가 아니어서 세금이 없어도 계산하는데 지장 없음

            //현재 아이템의 최종 금액 계산
            int totalPrice = supplyPrice + taxPrice;
            item.setTotalPrice(totalPrice);

            item.setActive(true);
            salesOrderItemRepository.save(item);

            //이번 총 계산금액 갱신
            totalOrderPrice += item.getTotalPrice();
        }
        //4. 전표 총액 반영
        savedSalesOrder.setTotalAmount(totalOrderPrice);

        return salesOrder;
    }


}
