package samosa_fos.de.dto.product;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProductRequest {

    private String code;
    private String productName;
    private String productNickname;
    private String variant;
    private String unit;
    private String brand;
    private String category;
    private Integer costPrice;
    private Integer salePrice;
    private Integer stockQuantity;
}
