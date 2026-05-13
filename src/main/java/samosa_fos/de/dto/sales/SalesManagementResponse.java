package samosa_fos.de.dto.sales;

import lombok.Getter;
import samosa_fos.de.domain.Customer;
import samosa_fos.de.domain.JobSite;
import samosa_fos.de.domain.SalesOrder;

import java.time.LocalDate;
import java.util.List;

@Getter
public class SalesManagementResponse {

    private final Long salesOrderId;
    private final LocalDate salesDate;
    private final Long customerId;
    private final String customerName;
    private final Long jobSiteId;
    private final String jobSiteName;
    private final String paymentType;
    private final String taxPolicy;
    private final String memo;
    private final Integer totalNetAmount;
    private final Integer totalTaxAmount;
    private final Integer totalAmount;
    private final Integer totalQuantity;
    private final String representativeProductName;
    private final Boolean editable;
    private final List<SalesManagementItemResponse> items;

    public SalesManagementResponse(SalesOrder salesOrder,
                                   Customer customer,
                                   JobSite jobSite,
                                   boolean editable,
                                   List<SalesManagementItemResponse> items) {
        this.salesOrderId = salesOrder.getId();
        this.salesDate = salesOrder.getSalesDate();
        this.customerId = salesOrder.getCustomerId();
        this.customerName = customer == null ? "고객 조회 불가" : customer.getName();
        this.jobSiteId = salesOrder.getJobSiteId();
        this.jobSiteName = jobSite == null ? "" : jobSite.getName();
        this.paymentType = salesOrder.getPaymentType();
        this.taxPolicy = salesOrder.getTaxPolicy();
        this.memo = salesOrder.getMemo();
        this.totalNetAmount = salesOrder.getTotalNetAmount();
        this.totalTaxAmount = salesOrder.getTotalTaxAmount();
        this.totalAmount = salesOrder.getTotalAmount();
        this.totalQuantity = items.stream()
                .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                .sum();
        this.representativeProductName = items.isEmpty() ? "" : items.get(0).getProductName();
        this.editable = editable;
        this.items = items;
    }
}
