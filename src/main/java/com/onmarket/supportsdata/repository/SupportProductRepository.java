package com.onmarket.supportsdata.repository;


import com.onmarket.supportsdata.domain.SupportProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportProductRepository extends JpaRepository<SupportProduct, Long>{
    boolean existsByServiceId(String serviceId);

}
