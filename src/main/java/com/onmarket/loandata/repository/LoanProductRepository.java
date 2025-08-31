package com.onmarket.loandata.repository;

import com.onmarket.loandata.domain.LoanProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanProductRepository extends JpaRepository<LoanProductEntity, String> {
}