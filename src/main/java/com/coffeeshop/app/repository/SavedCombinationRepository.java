package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.SavedCombination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedCombinationRepository extends JpaRepository<SavedCombination, Long> {
    List<SavedCombination> findByUserId(Long userId);
}
