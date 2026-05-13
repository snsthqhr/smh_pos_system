package samosa_fos.de.dto.customer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCustomerRequest {

    private String name;
    private String phone;
    private String address;
    private String memo;
}
