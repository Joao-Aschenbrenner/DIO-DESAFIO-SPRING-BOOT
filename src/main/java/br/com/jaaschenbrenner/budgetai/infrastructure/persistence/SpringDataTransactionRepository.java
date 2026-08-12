package br.com.jaaschenbrenner.budgetai.infrastructure.persistence;

import br.com.jaaschenbrenner.budgetai.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataTransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    List<TransactionEntity> findByCategory(Category category);
}
