package samosa_fos.de.dto.customer;

import lombok.Getter;
import samosa_fos.de.domain.Customer;

@Getter
public class CustomerResponse {

    private final Long id;
    private final String name;
    private final String phone;
    private final String address;
    private final String memo;

    public CustomerResponse(Customer customer) {
        this.id = customer.getId();
        this.name = customer.getName();
        this.phone = customer.getPhone();
        this.address = customer.getAddress();
        this.memo = customer.getMemo();
    }
}
