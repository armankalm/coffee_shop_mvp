package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Topping;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.repository.ToppingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ToppingService {

    private final ToppingRepository toppingRepository;

    public ToppingService(ToppingRepository toppingRepository) {
        this.toppingRepository = toppingRepository;
    }

    public Map<String, List<ToppingDto>> getAllGroupedByType() {
        return toppingRepository.findAllWithIncompatibilities().stream()
                .map(ToppingDto::from)
                .collect(Collectors.groupingBy(ToppingDto::getType));
    }

    /**
     * Validates that the given set of topping IDs contains no incompatible pairs.
     * Throws IllegalArgumentException if incompatible toppings are selected together.
     */
    public void validateCompatibility(Set<Long> toppingIds) {
        if (toppingIds == null || toppingIds.size() < 2) {
            return;
        }
        List<Topping> toppings = toppingRepository.findAllById(toppingIds);
        // Build a set of selected IDs for O(1) lookup
        Set<Long> selectedIds = new java.util.HashSet<>(toppingIds);
        for (Topping topping : toppings) {
            for (Topping incompatible : topping.getIncompatibleWith()) {
                if (selectedIds.contains(incompatible.getId())) {
                    throw new IllegalArgumentException(
                            "Incompatible toppings selected: '" + topping.getName()
                            + "' and '" + incompatible.getName() + "'");
                }
            }
        }
    }
}
