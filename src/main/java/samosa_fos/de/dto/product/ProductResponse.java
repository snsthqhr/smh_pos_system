package samosa_fos.de.dto.product;

import lombok.Getter;
import samosa_fos.de.domain.Product;

@Getter
public class ProductResponse {

    private final Long id;
    private final String code;
    private final String productName;
    private final String productNickname;
    private final String variant;
    private final String unit;
    private final String brand;
    private final String category;
    private final Integer costPrice;
    private final Integer salePrice;
    private final Integer stockQuantity;

    public ProductResponse(Product product) {
        this.id = product.getId();
        this.code = product.getCode();
        this.productName = product.getProductName();
        this.productNickname = product.getProductNickname();
        this.variant = product.getVariant();
        this.unit = product.getUnit();
        this.brand = product.getBrand();
        this.category = product.getCategory();
        this.costPrice = product.getCostPrice();
        this.salePrice = product.getSalePrice();
        this.stockQuantity = product.getStockQuantity();
    }
}
