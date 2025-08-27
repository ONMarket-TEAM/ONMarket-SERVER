package com.onmarket.business.repository;

import com.onmarket.business.domain.Business;
import com.onmarket.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BusinessRepository extends JpaRepository<Business, Long> {
    List<Business> findByMember(Member member);
}
