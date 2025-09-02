package com.onmarket.fssdata.repository;


import com.onmarket.fssdata.domain.CreditLoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditLoanProductRepository extends JpaRepository<CreditLoanProduct, Long> {
    List<CreditLoanProduct> findByFinCoNo(String finCoNo);
    List<CreditLoanProduct> findByKorCoNmContaining(String korCoNm);
    CreditLoanProduct findByFinPrdtCd(String finPrdtCd);
}
