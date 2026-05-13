package samosa_fos.de.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import samosa_fos.de.domain.Customer;
import samosa_fos.de.dto.customer.CreateCustomerRequest;
import samosa_fos.de.dto.customer.CustomerResponse;
import samosa_fos.de.repository.CustomerRepository;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    public List<CustomerResponse> searchCustomers(@RequestParam(required = false) String keyword) {
        String normalizedKeyword = normalize(keyword);
        List<Customer> customers = normalizedKeyword.isBlank()
                ? customerRepository.findByActiveTrue()
                : customerRepository.findByNameContaining(normalizedKeyword);

        return customers.stream()
                .limit(20)
                .map(CustomerResponse::new)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@RequestBody CreateCustomerRequest request) {
        if (request == null || normalize(request.getName()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "고객명은 필수입니다.");
        }

        Customer customer = new Customer();
        customer.setName(normalize(request.getName()));
        customer.setPhone(normalizeNullable(request.getPhone()));
        customer.setAddress(normalizeNullable(request.getAddress()));
        customer.setMemo(normalizeNullable(request.getMemo()));
        customer.setActive(true);

        return new CustomerResponse(customerRepository.save(customer));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }
}
