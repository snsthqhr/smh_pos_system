package samosa_fos.de.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import samosa_fos.de.domain.Product;
import samosa_fos.de.dto.product.CreateProductRequest;
import samosa_fos.de.dto.product.ProductResponse;
import samosa_fos.de.repository.ProductRepository;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductResponse> searchProducts(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String category) {

        String normalizedKeyword = normalize(keyword);
        String normalizedCategory = normalize(category);

        List<Product> products;
        if (normalizedKeyword.isBlank()) {
            products = normalizedCategory.isBlank()
                    ? productRepository.findAll().stream().limit(30).toList()
                    : productRepository.findByCategory(normalizedCategory).stream().limit(30).toList();
        } else {
            products = productRepository.findAll().stream()
                    .map(product -> new ProductSearchResult(product, searchScore(product, normalizedKeyword, normalizedCategory)))
                    .filter(result -> result.score() > 0)
                    .sorted(Comparator.comparingInt(ProductSearchResult::score).reversed())
                    .limit(30)
                    .map(ProductSearchResult::product)
                    .toList();
        }

        return products.stream()
                .map(ProductResponse::new)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody CreateProductRequest request) {
        validateCreateRequest(request);

        String code = normalize(request.getCode());
        if (code.isBlank()) {
            code = generateProductCode();
        }
        productRepository.findByCode(code).ifPresent(product -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 상품코드입니다.");
        });

        Product product = new Product();
        product.setCode(code);
        product.setProductName(normalize(request.getProductName()));
        product.setProductNickname(defaultText(request.getProductNickname(), product.getProductName()));
        product.setVariant(normalizeNullable(request.getVariant()));
        product.setUnit(defaultText(request.getUnit(), product.getVariant()));
        product.setBrand(normalizeNullable(request.getBrand()));
        product.setCategory(defaultText(request.getCategory(), "기타"));
        product.setCostPrice(defaultNumber(request.getCostPrice()));
        product.setSalePrice(defaultNumber(request.getSalePrice()));
        product.setStockQuantity(defaultNumber(request.getStockQuantity()));

        return new ProductResponse(productRepository.save(product));
    }

    private void validateCreateRequest(CreateProductRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상품 정보가 필요합니다.");
        }
        if (normalize(request.getProductName()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "품명은 필수입니다.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String defaultText(String value, String defaultValue) {
        String normalized = normalize(value);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private Integer defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String generateProductCode() {
        String code;
        do {
            long millis = System.currentTimeMillis();
            int suffix = ThreadLocalRandom.current().nextInt(100, 1000);
            code = "P" + millis + suffix;
        } while (productRepository.findByCode(code).isPresent());

        return code;
    }

    private int searchScore(Product product, String keyword, String category) {
        String haystack = compact(
                safe(product.getCode()) + " " +
                        safe(product.getProductName()) + " " +
                        safe(product.getProductNickname()) + " " +
                        safe(product.getVariant()) + " " +
                        safe(product.getUnit()) + " " +
                        safe(product.getBrand()) + " " +
                        safe(product.getCategory())
        );
        String compactKeyword = compact(keyword);

        int score = 0;
        if (!compactKeyword.isBlank() && haystack.contains(compactKeyword)) {
            score += 100;
        }

        for (String token : keyword.split("\\s+")) {
            String compactToken = compact(token);
            if (!compactToken.isBlank() && haystack.contains(compactToken)) {
                score += 10;
            }
        }

        if (!category.isBlank() && category.equals(product.getCategory())) {
            score += 5;
        }

        return score;
    }

    private String compact(String value) {
        return safe(value).replaceAll("\\s+", "").toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record ProductSearchResult(Product product, int score) {
    }
}
