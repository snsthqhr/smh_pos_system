package samosa_fos.de.service;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import samosa_fos.de.domain.ArTx;
import samosa_fos.de.dto.sales.CustomerArBalanceResponse;
import samosa_fos.de.dto.sales.LedgerSearchRequest;
import samosa_fos.de.repository.ArTxRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArService {


    private final ArTxRepository arTxRepository;

    public ArService(ArTxRepository arTxRepository){

        this.arTxRepository = arTxRepository;
    }



    public CustomerArBalanceResponse getCurrentArBalanceResponse (Long customerId){

        List<ArTx> arTxList = new ArrayList<>();

        //고객 아이디 존재하는지 조회
         validateArBalanceCustomerId(customerId);
         //고객의 미수금을 전부 조회
        arTxList = arTxRepository.findByCustomerIdAndActiveTrue(customerId);

        //고객의 미수금을 합산
        int currentArBalance = arTxList.stream()
                .mapToInt(arTx ->arTx.getAmount() == null ? 0 : arTx.getAmount() )
                .sum();

        //4.DTO 변환
        return new CustomerArBalanceResponse(customerId, currentArBalance);
    }


    private void validateArBalanceCustomerId(Long customerId) {

        if (customerId == null) {
            throw new IllegalArgumentException("customerId는 필수입니다.");
        }
    }

}
