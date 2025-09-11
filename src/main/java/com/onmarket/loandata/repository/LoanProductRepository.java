package com.onmarket.loandata.repository;

import com.onmarket.loandata.domain.LoanProduct;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    // sequence(고유번호)로 찾기 (반환 타입을 Optional로 변경하여 안정성 높임)
    Optional<LoanProduct> findBySequence(String sequence);

    // 상품명으로 검색
    List<LoanProduct> findByProductNameContaining(String productName);
    @Query("""
        SELECT sp.id
        FROM SupportProduct sp
        WHERE (sp.keywords LIKE %:sidoPattern% OR sp.departmentName LIKE %:sidoPattern%)
          AND (sp.keywords LIKE %:sigunguPattern% OR sp.departmentName LIKE %:sigunguPattern%)
    """)
    List<Long> findIdsByRegionPatterns(@Param("sidoPattern") String sidoPattern,
                                       @Param("sigunguPattern") String sigunguPattern);

}