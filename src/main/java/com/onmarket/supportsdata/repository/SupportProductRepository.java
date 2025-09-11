package com.onmarket.supportsdata.repository;


import com.onmarket.fssdata.domain.CreditLoanProduct;
import com.onmarket.supportsdata.domain.SupportProduct;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportProductRepository extends JpaRepository<SupportProduct, Long>{
    boolean existsByServiceId(String serviceId);


    @Query("""
    SELECT sp.id
    FROM SupportProduct sp
    WHERE (sp.keywords LIKE %:sidoPattern% OR sp.departmentName LIKE %:sidoPattern%)
      AND (sp.keywords LIKE %:sigunguPattern% OR sp.departmentName LIKE %:sigunguPattern%)
""")
    List<Long> findIdsByRegionPatterns(@Param("sidoPattern") String sidoPattern,
                                       @Param("sigunguPattern") String sigunguPattern);

    List<SupportProduct> findByUpdatedAtAfter(LocalDateTime dateTime);
}
