package samosa_fos.de.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional(readOnly = true)
public class StatementDocxService {

    private final StatementService statementService;

    public  StatementDocxService(StatementService statementService) {
        this.statementService = statementService;
    }

    

}
