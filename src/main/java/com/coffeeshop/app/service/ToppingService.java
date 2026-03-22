package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Topping;
import com.coffeeshop.app.domain.ToppingType;
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

    public Map<ToppingType, List<ToppingDto>> getAllGroupedByType() {
        return toppingRepository.findAll().stream()
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
            // Check both the owning side and the inverse side to handle asymmetric DB entries
            for (Topping incompatible : topping.getIncompatibleWith()) {
                if (selectedIds.contains(incompatible.getId())) {
                    throw new IllegalArgumentException(
                            "Incompatible toppings selected: '" + topping.getName()
                            + "' and '" + incompatible.getName() + "'");
                }
            }
        }
        // Check reverse direction: for each pair (A, B) where B lists A as incompatible but A does not list B
        for (int i = 0; i < toppings.size(); i++) {
            Topping a = toppings.get(i);
            Set<Long> aIncompat = a.getIncompatibleWith().stream()
                    .map(Topping::getId).collect(java.util.stream.Collectors.toSet());
            for (int j = i + 1; j < toppings.size(); j++) {
                Topping b = toppings.get(j);
                if (!aIncompat.contains(b.getId())) {
                    // A doesn't declare B incompatible; check if B declares A incompatible
                    boolean bDeclaresAIncompat = b.getIncompatibleWith().stream()
                            .anyMatch(t -> t.getId().equals(a.getId()));
                    if (bDeclaresAIncompat) {
                        throw new IllegalArgumentException(
                                "Incompatible toppings selected: '" + b.getName()
                                + "' and '" + a.getName() + "'");
                    }
                }
            }
        }
    }
}
