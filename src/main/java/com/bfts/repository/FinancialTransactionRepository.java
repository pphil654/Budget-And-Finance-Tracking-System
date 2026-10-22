package com.bfts.repository;

import com.bfts.entity.FinancialTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransaction, Long> {

    List<FinancialTransaction> findByProjectId(Long projectId);

    List<FinancialTransaction> findByTransactionType(String transactionType);

    // Get total expenses for all projects
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.transactionType = 'EXPENSE'
    """)
    BigDecimal getTotalExpenses();

    // Get total expenses for a specific project
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM FinancialTransaction t
        WHERE t.project.id = :projectId
        AND t.transactionType = 'EXPENSE'
    """)
    BigDecimal getTotalExpensesByProjectId(Long projectId);
}