package samosa_fos.de.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.*;
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

    public List<SalesHistoryResponse> getSalesHistory(SalesHistorySearchRequest request) {

        validateRequest(request);

        List<SalesOrder> salesOrders;

        if (request.getStartDate() != null && request.getEndDate() != null) {
            salesOrders = salesOrderRepository
                    .findByCustomerIdAndSalesDateBetweenAndActiveTrue(
                            request.getCustomerId(),
                            request.getStartDate(),
                            request.getEndDate()
                    );
        } else {
            salesOrders = salesOrderRepository
                    .findByCustomerIdAndActiveTrue(request.getCustomerId());
        }

        List<SalesHistoryResponse> responses = new ArrayList<>();

        for (SalesOrder order : salesOrders) {
            List<SalesOrderItem> items =
                    salesOrderItemRepository.findBySalesOrderId(order.getId());

            List<SalesHistoryItemResponse> itemResponses = new ArrayList<>();

            for (SalesOrderItem item : items) {
                Product product = productRepository
                        .findById(item.getProductId())
                        .orElse(null);

                itemResponses.add(new SalesHistoryItemResponse(
                        product != null ? product.getProductName() : "상품 조회 불가",
                        product != null ? product.getUnit() : null,
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getReturnQuantity(),
                        item.getSupplyPrice(),
                        item.getTaxPrice(),
                        item.getTotalPrice(),
                        item.getId()
                ));
            }

            responses.add(new SalesHistoryResponse(
                    order.getId(),
                    order.getSalesDate(),
                    order.getPaymentType(),
                    order.getMemo(),
                    order.getTotalNetAmount(),
                    order.getTotalTaxAmount(),
                    order.getTotalAmount(),
                    itemResponses
            ));
        }

        return responses;
    }

    private void validateRequest(SalesHistorySearchRequest request) {
        if (request.getCustomerId() == null) {
            throw new IllegalArgumentException("customerId는 필수입니다.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null &&
                request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }
}