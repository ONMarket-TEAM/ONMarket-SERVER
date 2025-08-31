package com.onmarket.loandata.repository;

import com.onmarket.loandata.domain.LoanProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    // sequence(고유번호)로 찾기 (반환 타입을 Optional로 변경하여 안정성 높임)
    Optional<LoanProduct> findBySequence(String sequence);

    // 상품명으로 검색
    List<LoanProduct> findByProductNameContaining(String productName);

    // 취급기관명으로 검색
    List<LoanProduct> findByHandlingInstitutionContaining(String handlingInstitution);

    // 상품 카테고리로 검색
    List<LoanProduct> findByProductCategory(String productCategory);

    // 대출한도 검색
    List<LoanProduct> findByLoanLimitContaining(String loanLimit);

    // sequence 존재 여부 확인
    boolean existsBySequence(String sequence);

    // 최근 추가된 상품들
    @Query("SELECT p FROM LoanProduct p ORDER BY p.createdAt DESC")
    List<LoanProduct> findRecentProducts(Pageable pageable);


    List<LoanProduct> findByOfferingInstitutionContaining(String offeringInstitution);

    List<LoanProduct> findByTargetContaining(String target);

    @Query("SELECT DISTINCT p.productCategory FROM LoanProduct p WHERE p.productCategory IS NOT NULL")
    List<String> findAllCategories();


    @Query("SELECT DISTINCT p.offeringInstitution FROM LoanProduct p WHERE p.offeringInstitution IS NOT NULL ORDER BY p.offeringInstitution")
    List<String> findAllInstitutions();
}