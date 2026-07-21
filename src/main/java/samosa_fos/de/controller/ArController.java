package samosa_fos.de.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import samosa_fos.de.dto.sales.CustomerArBalanceResponse;
import samosa_fos.de.service.ArService;

@RestController
@RequestMapping("/api/customers")
public class ArController {

    private final ArService arService;

    public ArController(ArService arService) {
        this.arService = arService;
    }

    @GetMapping("/{customerId}/ar-balance")
    public CustomerArBalanceResponse getCurrentArBalance(@PathVariable Long customerId) {
        return arService.getCurrentArBalanceResponse(customerId);
    }
}
