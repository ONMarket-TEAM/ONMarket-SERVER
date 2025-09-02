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

    // 대출한도 검색
    List<LoanProduct> findByLoanLimitContaining(String loanLimit);

    // sequence 존재 여부 확인
    boolean existsBySequence(String sequence);

    // 최근 추가된 상품들
    @Query("SELECT p FROM LoanProduct p ORDER BY p.createdAt DESC")
    List<LoanProduct> findRecentProducts(Pageable pageable);

    List<LoanProduct> findByOfferingInstitutionContaining(String offeringInstitution);

    List<LoanProduct> findByTargetContaining(String target);

    @Query("SELECT DISTINCT p.offeringInstitution FROM LoanProduct p WHERE p.offeringInstitution IS NOT NULL ORDER BY p.offeringInstitution")
    List<String> findAllInstitutions();

    /**
     * trgt, suprTgtDtlCond 컬럼에서 특정 키워드를 포함하는 상품을 모두 검색
     * @param keyword1 사업자
     * @param keyword2 기업
     * @param keyword3 소상공인
     * @param keyword4 청년 창업자
     * @return 필터링된 LoanProduct 리스트
     */
    @Query("SELECT p FROM LoanProduct p " +
            "WHERE p.target LIKE %:keyword1% OR p.target LIKE %:keyword2% OR p.target LIKE %:keyword3% OR p.target LIKE %:keyword4% " +
            "OR p.specialTargetConditions LIKE %:keyword1% OR p.specialTargetConditions LIKE %:keyword2% OR p.specialTargetConditions LIKE %:keyword3% OR p.specialTargetConditions LIKE %:keyword4%")
    List<LoanProduct> findByKeywordsInTrgtOrSuprTgtDtlCond(String keyword1, String keyword2, String keyword3, String keyword4);
}