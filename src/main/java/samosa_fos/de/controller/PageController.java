package samosa_fos.de.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/pos"})
    public String pos() {
        return "pos";
    }

    @GetMapping("/sales-management")
    public String salesManagement() {
        return "sales-management";
    }

    @GetMapping("/ar-management")
    public String arManagement() {
        return "ar-management";
    }
}
