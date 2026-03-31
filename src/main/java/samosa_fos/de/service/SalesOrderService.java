package samosa_fos.de.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.*;
import samosa_fos.de.dto.sales.CreateSalesOrderItemRequest;
import samosa_fos.de.dto.sales.CreateSalesOrderRequest;
import samosa_fos.de.repository.*;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final CustomerPriceRepository customerPriceRepository;
    private final ProductRepository productRepository;

    public SalesOrderService(SalesOrderRepository salesOrderRepository,
                             SalesOrderItemRepository salesOrderItemRepository,
                              CustomerPriceRepository customerPriceRepository,
                             ProductRepository productRepository){
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.customerPriceRepository = customerPriceRepository;
        this.productRepository = productRepository;
    }


    //기본베이스는 dto를 활용하여 주입받는다
    public SalesOrder createSalesOrder(CreateSalesOrderRequest request){
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

        int totalNetAmount = 0;//공급가액
        int totalTaxAmount = 0;//공급가액*10%
        int totalAmount = 0;


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

            item.setUnitPrice(determineUnitPrice(request.getCustomerId(),
                    request.getJobSiteId(),
                    item.getProductId(),
                    itemRequest.getUnitPrice()));


            //제품의 반품 내역
            //제품의 판매 시점에서는 반품이 0인게 당연한것
            item.setReturnQuantity(0);

            // 현재 제품의 공급가
            int curItemSupplyPrice = item.getUnitPrice()* item.getQuantity();
            item.setSupplyPrice(curItemSupplyPrice);

            //이 주문의 금액의 공급가
            totalNetAmount += curItemSupplyPrice;

            //현재아이템의 세금계산
            // 지금은 간단히 taxPolicy가 NO_TAX면 0, 아니면 10%처리한다
            int curItemTaxPrice = 0;
            if("ADD_VAT".equals(request.getTaxPolicy())){
                curItemTaxPrice = (int) (curItemSupplyPrice * 0.1);
            }
            item.setTaxPrice(curItemTaxPrice);//만약 ADD_VAT가 아니어서 세금이 없어도 계산하는데 지장 없음
            //이 주문의 세금만 총액
            totalTaxAmount += curItemTaxPrice;

            //현재 아이템의 최종 금액 계산
            int curItemTotalPrice = curItemSupplyPrice + curItemTaxPrice;
            item.setTotalPrice(curItemTotalPrice);


            item.setActive(true);
            salesOrderItemRepository.save(item);

            //이번 총 계산금액 갱신
            totalAmount += item.getTotalPrice();

            //아이템의 가격을 CustomerPrcie로 저장 할 지 안 할지 결정하는 함수 호출
            saveCustomerPricePolicy(
                    request.getCustomerId(),
                    request.getJobSiteId(),
                    item.getProductId(),
                    item.getUnitPrice(),
                    request.getPriceApplyPolicy());
        }
        //4. 전표 총액 반영
        savedSalesOrder.setTotalNetAmount(totalNetAmount);
        savedSalesOrder.setTotalTaxAmount(totalTaxAmount);
        savedSalesOrder.setTotalAmount(totalAmount);

        return savedSalesOrder;
    }

    public Integer determineUnitPrice(Long customerId, Long jobSiteId, Long productId, Integer requestUnitPrice){

        // 1. 사용자가 직접 가격 입력한 경우
        if(requestUnitPrice != null) {
            return requestUnitPrice;
        }
        // 2. 현장 가격 조회
        if(jobSiteId != null){
            CustomerPrice jobSitePrice = customerPriceRepository.findByCustomerIdAndProductIdAndJobSiteIdAndActiveTrue(customerId,productId,jobSiteId)
                    .orElse(null);
            if(jobSitePrice !=null){
                return jobSitePrice.getPrice();
            }

        }

        //3. 고객 기본 가격 조회
       CustomerPrice customerDefaultPrice = customerPriceRepository
               .findByCustomerIdAndProductIdAndJobSiteIdIsNullAndActiveTrue(customerId,productId)
               .orElse(null);
       if (customerDefaultPrice!=null){
           return customerDefaultPrice.getPrice();
       }


        //4. 상품 기본 가격 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(()->new IllegalArgumentException("해당 상품이 존재하지 않습니다. + productId"+productId));
        if (product.getSalePrice()==null){
            throw new IllegalArgumentException("상품 기본 가격이 설정되지 않았습니다. productId=" + productId);
        }
        return product.getSalePrice();

    }


    private void saveCustomerPricePolicy(
            Long customerId,
            Long jobSiteId,
            Long productId,
            Integer unitPrice,
            String pricePolicy){

        if ("ONE_TIME_ONLY".equals(pricePolicy)){
            return;
        }

        if (pricePolicy.equals("SAVE_PRICE")){
            CustomerPrice customerPrice = new CustomerPrice();
            customerPrice.setCustomerId();
            customerPrice.setPrice();
            customerPrice.setActive();
            customerPrice.setProductId();

            CustomerPrice savedCustomerPrice = customerPriceRepository.save(customerPrice);

        }
        else {

        }
    }

}
