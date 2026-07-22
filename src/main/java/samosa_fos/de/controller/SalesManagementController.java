package samosa_fos.de.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import samosa_fos.de.domain.Customer;
import samosa_fos.de.domain.JobSite;
import samosa_fos.de.domain.Product;
import samosa_fos.de.domain.SalesOrder;
import samosa_fos.de.domain.SalesOrderItem;
import samosa_fos.de.dto.sales.SalesManagementItemResponse;
import samosa_fos.de.dto.sales.SalesManagementResponse;
import samosa_fos.de.repository.CustomerRepository;
import samosa_fos.de.repository.JobSiteRepository;
import samosa_fos.de.repository.ProductRepository;
import samosa_fos.de.repository.SalesOrderItemRepository;
import samosa_fos.de.repository.SalesOrderRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/sales-management")
public class SalesManagementController {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final JobSiteRepository jobSiteRepository;

    public SalesManagementController(SalesOrderRepository salesOrderRepository,
                                     SalesOrderItemRepository salesOrderItemRepository,
                                     ProductRepository productRepository,
                                     CustomerRepository customerRepository,
                                     JobSiteRepository jobSiteRepository) {
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.jobSiteRepository = jobSiteRepository;
    }

    @GetMapping("/sales")
    public List<SalesManagementResponse> searchSales(@RequestParam(required = false) Long customerId,
                                                     @RequestParam(required = false) LocalDate startDate,
                                                     @RequestParam(required = false) LocalDate endDate,
                                                     @RequestParam(required = false) String productKeyword,
                                                     @RequestParam(required = false) String paymentType,
                                                     @RequestParam(required = false) String taxPolicy) {
        List<SalesOrder> orders = findOrders(customerId, startDate, endDate);

        return orders.stream()
                .filter(order -> matchesPaymentType(order, paymentType))
                .filter(order -> matchesTaxPolicy(order, taxPolicy))
                .filter(order -> matchesProductKeyword(order, productKeyword))
                .sorted(Comparator.comparing(SalesOrder::getSalesDate).reversed()
                        .thenComparing(SalesOrder::getId, Comparator.reverseOrder()))
                .limit(100)
                .map(this::toResponse)
                .toList();
    }

    private List<SalesOrder> findOrders(Long customerId, LocalDate startDate, LocalDate endDate) {
        if (customerId != null && startDate != null && endDate != null) {
            return salesOrderRepository.findByCustomerIdAndSalesDateBetweenAndActiveTrue(customerId, startDate, endDate);
        }
        if (customerId != null) {
            return salesOrderRepository.findByCustomerIdAndActiveTrue(customerId);
        }
        if (startDate != null && endDate != null) {
            return salesOrderRepository.findBySalesDateBetweenAndActiveTrue(startDate, endDate);
        }
        return salesOrderRepository.findAll().stream()
                .filter(order -> Boolean.TRUE.equals(order.getActive()))
                .toList();
    }

    private SalesManagementResponse toResponse(SalesOrder order) {
        Customer customer = customerRepository.findById(order.getCustomerId()).orElse(null);
        JobSite jobSite = order.getJobSiteId() == null
                ? null
                : jobSiteRepository.findById(order.getJobSiteId()).orElse(null);
        List<SalesManagementItemResponse> items = salesOrderItemRepository.findBySalesOrderId(order.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .map(item -> {
                    Product product = productRepository.findById(item.getProductId()).orElse(null);
                    return new SalesManagementItemResponse(item, product);
                })
                .toList();

        // 2026-07-22 정책 변경:
        // 수금 존재 여부만으로 판매수정을 막지 않는다.
        // 반품이 있는 전표만 잠그고, 수금액 초과 검증은 저장 시 SalesOrderUpdateService에서 최종 처리한다.
        boolean editable = !salesOrderItemRepository.existsBySalesOrderIdAndReturnQuantityGreaterThan(order.getId(), 0);

        return new SalesManagementResponse(order, customer, jobSite, editable, items);
    }

    private boolean matchesPaymentType(SalesOrder order, String paymentType) {
        return paymentType == null || paymentType.isBlank() || paymentType.equals(order.getPaymentType());
    }

    private boolean matchesTaxPolicy(SalesOrder order, String taxPolicy) {
        return taxPolicy == null || taxPolicy.isBlank() || taxPolicy.equals(order.getTaxPolicy());
    }

    private boolean matchesProductKeyword(SalesOrder order, String productKeyword) {
        String keyword = normalize(productKeyword);
        if (keyword.isBlank()) {
            return true;
        }

        return salesOrderItemRepository.findBySalesOrderId(order.getId()).stream()
                .filter(item -> Boolean.TRUE.equals(item.getActive()))
                .map(SalesOrderItem::getProductId)
                .map(productRepository::findById)
                .anyMatch(product -> product
                        .map(value -> normalize(value.getCode()).contains(keyword)
                                || normalize(value.getProductName()).contains(keyword)
                                || normalize(value.getProductNickname()).contains(keyword))
                        .orElse(false));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", "").toLowerCase();
    }
}
