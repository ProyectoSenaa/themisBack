package co.sena.edu.themis.Strategy;

import co.sena.edu.themis.Entity.Novelty;

public interface NoveltyValidationStrategy {

    /**
     * Validates and processes a novelty based on its type and business rules
     * @param novelty The novelty to validate and process
     * @return The processed novelty with updated status
     */
    Novelty validateAndProcess(Novelty novelty);

    /**
     * Determines if this strategy can handle the given novelty type
     * @param noveltyType The type of novelty
     * @return true if this strategy can handle the novelty type
     */
    boolean canHandle(String noveltyType);
}
