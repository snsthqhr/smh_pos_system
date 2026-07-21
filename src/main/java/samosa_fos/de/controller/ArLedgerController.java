package samosa_fos.de.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import samosa_fos.de.domain.Customer;
import samosa_fos.de.dto.sales.LedgerRowDto;
import samosa_fos.de.dto.sales.LedgerSearchRequest;
import samosa_fos.de.repository.CustomerRepository;
import samosa_fos.de.service.LedgerService;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/ar-ledger")
public class ArLedgerController {

    private final LedgerService ledgerService;
    private final CustomerRepository customerRepository;

    public ArLedgerController(LedgerService ledgerService,
                              CustomerRepository customerRepository) {
        this.ledgerService = ledgerService;
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<LedgerRowDto> getArLedgerRows(@RequestParam(required = false) Long customerId,
                                              @RequestParam(required = false) LocalDate startDate,
                                              @RequestParam(required = false) LocalDate endDate) {
        List<Customer> customers = findCustomers(customerId);

        return customers.stream()
                .flatMap(customer -> getRowsForCustomer(customer, startDate, endDate).stream())
                .sorted(Comparator.comparing(LedgerRowDto::getTxDate)
                        .thenComparing(row -> row.getCustomerName() == null ? "" : row.getCustomerName())
                        .thenComparing(row -> row.getSalesOrderId() == null ? Long.MAX_VALUE : row.getSalesOrderId()))
                .toList();
    }

    private List<Customer> findCustomers(Long customerId) {
        if (customerId != null) {
            return customerRepository.findById(customerId)
                    .filter(customer -> Boolean.TRUE.equals(customer.getActive()))
                    .map(List::of)
                    .orElse(List.of());
        }
        return customerRepository.findByActiveTrue();
    }

    private List<LedgerRowDto> getRowsForCustomer(Customer customer, LocalDate startDate, LocalDate endDate) {
        LedgerSearchRequest request = new LedgerSearchRequest();
        request.setCustomerId(customer.getId());
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setShowAll(true);

        return ledgerService.getLedgerRows(request).stream()
                .peek(row -> row.setCustomerName(customer.getName()))
                .toList();
    }
}
