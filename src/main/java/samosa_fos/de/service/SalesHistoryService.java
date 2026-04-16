package samosa_fos.de.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.SalesHistoryItemResponse;
import samosa_fos.de.dto.sales.SalesHistoryResponse;
import samosa_fos.de.dto.sales.SalesHistorySearchRequest;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SalesHistoryService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ProductRepository productRepository;

    public SalesHistoryService(SalesOrderRepository salesOrderRepository,
                               SalesOrderItemRepository salesOrderItemRepository,
                               ProductRepository productRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.productRepository = productRepository;
    }

    public List<SalesHistoryResponse> getSalesHistory(SalesHistorySearchRequest request){

        validateRequest(request);

        List<SalesOrder> salesOrders;
        //날짜가 주어진 경우
        if(request.getStartDate()!=null &&request.getEndDate()!=null){
            salesOrders = salesOrderRepository.findByCustomerIdAndSalesDateBetweenAndActiveTrue(
                    request.getCustomerId(),
                    request.getStartDate(),
                    request.getEndDate()
            );
        }
        //날짜가 주어지지 않았을경우에는 단순히 고객아이디로 조회한다.
        else {
            salesOrders = salesOrderRepository
                    .findByCustomerIdAndActiveTrue(request.getCustomerId());
        }

        List<SalesHistoryItemResponse> itemResponses = new ArrayList<>();

        for (SalesOrderItem item : items) {
            Product product = productRepository
                    .findById(item.getProductId())
                    .orElse(null);

            itemResponses.add(new SalesHistoryItemResponse(
                    product != null ? product.getProductName() :"상품 조회 불가",
                    pro
            ))
        }

    }
        private void validateRequest(SalesHistorySearchRequest request){
            if(request.getCustomerId()== null) {
                throw new IllegalArgumentException("customerId는 필수입니다.");
            }
            if(request.getStartDate()!=null && request.getEndDate()!=null&&request.getStartDate().isAfter(request.getEndDate())){
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }
        }

}
