package com.onmarket.fssdata.repository;

import com.onmarket.fssdata.domain.CreditLoanOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditLoanOptionRepository extends JpaRepository<CreditLoanOption, Long> {
    List<CreditLoanOption> findByFinPrdtCd(String finPrdtCd);
    List<CreditLoanOption> findByFinCoNo(String finCoNo);

    // 중복 체크용 메서드 추가
    CreditLoanOption findByFinCoNoAndFinPrdtCdAndCrdtLendRateType(
            String finCoNo, String finPrdtCd, String crdtLendRateType);

    // 특정 상품+금리타입 조합 존재 확인
    boolean existsByFinCoNoAndFinPrdtCdAndCrdtLendRateType(
            String finCoNo, String finPrdtCd, String crdtLendRateType);
}