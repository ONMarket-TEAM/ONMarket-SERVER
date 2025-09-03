package com.onmarket.supportsdata.repository;

import com.onmarket.supportsdata.domain.SupportProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportProductRepository extends JpaRepository<SupportProduct, String> {
}
