package samosa_fos.de.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import samosa_fos.de.dto.sales.RegisterPaymentRequest;

@Service
@Getter
@Setter
public class PaymentService {




    public void registerPayment(RegisterPaymentRequest request){


        //수금등록 request로 부터 온 데이터가 이상이 없는지 확인한다.
        if(request.getCustomerId()==null){
            throw new IllegalArgumentException("customerid는 필수입니다.");
        }
        if(request.getAmount()<=0|| request.getAmount() ==null){
            throw new IllegalArgumentException("수금 금액이 잘못 되었습니다.");
        }
        if(request.getPaymentMethod()==null ||request.getPaymentMethod().isBlank()){
            throw  new IllegalArgumentException("결제 방식은 필수입니다.");
        }


    }
}
