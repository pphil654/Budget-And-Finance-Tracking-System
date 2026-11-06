package com.bfts.repository;

import com.bfts.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByProjectId(Long projectId);

    List<Payment> findByStatusIgnoreCase(String status);

    List<Payment> findByProjectIdAndStatusIgnoreCase(
            Long projectId,
            String status
    );
}
