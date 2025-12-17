package org.mitre.synthea.codebase.generated.Stock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The `Stock` class represents a stock management system where each item (represented by NDCs) 
 * is associated with a boolean value indicating its availability (true for in-stock, false for out-of-stock).
 * It simulates a stock management system by providing functionality to initialize the stock with random values 
 * and retrieve the stock map for further operations.
 * 
 * This class encapsulates a private `mapStock` field, which is a HashMap storing NDC codes 
 * as keys and their corresponding stock status as boolean values. It includes methods to:
 * - Initialize the stock map with a list of NDCs by randomly assigning availability (true or false).
 * - Retrieve the stock map using a getter method.
 * 
 * The class also calculates and prints the percentage distribution of true (in-stock) and false
 * (out-of-stock) values after initializing the map. The distribution percentages provide insights
 * into the proportion of NDCs marked as in-stock and out-of-stock.
 *
 * Usage:
 * - Call `initializeMapStock()` to initialize the stock status for a list of NDC codes.
 * - Access the stock map using `getMapStock()`.
 *
 * Example:
 * ```
 * Stock stock = new Stock();
 * stock.initializeMapStock(ndcList);
 * HashMap&lt;String, Boolean&gt; stockMap = stock.getMapStock();
 * ```
 */

public class Stock {

    /**
     * A private HashMap to store the stock status of items.
     * Key: A string representing the National Drug Code (NDC).
     * Value: A boolean indicating availability (`true` if in stock, `false` otherwise).
     */
    private HashMap<String, Boolean> mapStock; // Private due to encapsulation principles

    /**
     * Initializes the stock map with a list of NDCs, assigning each a random boolean value 
     * to indicate its stock status.
     * 
     * @param ndcList A list of strings representing NDCs to initialize the stock map.
     *                Each NDC is mapped to a randomly assigned `true` or `false` value.
     */
    public void initializeMapStock(List<String> ndcList) {
        // Log the initialization process
        System.out.println("Initializing the stock map with the following NDC list: " + ndcList);

        // Initialize the stock map
        mapStock = new HashMap<>();
        Random random = new Random(); // Random generator for assigning stock values
        int trueCount = 0;  // Counter for the number of `true` entries
        int falseCount = 0; // Counter for the number of `false` entries

        // Loop through the NDC list and assign random boolean values
        for (String ndc : ndcList) {
            boolean value = random.nextBoolean(); // Randomly assign `true` or `false`
            mapStock.put(ndc, value); // Add the NDC and its value to the map
            if (value) {
                trueCount++; // Increment the count for `true` values
            } else {
                falseCount++; // Increment the count for `false` values
            }
        }

        // Calculate percentages for `true` and `false` values
        int total = trueCount + falseCount; // Total number of entries
        double truePercentage = (trueCount * 100.0) / total; // Percentage of `true` values
        double falsePercentage = (falseCount * 100.0) / total; // Percentage of `false` values

        // Log the percentages
        System.out.println("Percentage of true: " + truePercentage + "%");
        System.out.println("Percentage of false: " + falsePercentage + "%");
    }

    /**
     * Provides access to the stock map (`mapStock`).
     * 
     * @return The `mapStock` HashMap containing NDCs and their availability status.
     */
    public HashMap<String, Boolean> getMapStock() {
        return mapStock;
    }

    /**
     * Main method to demonstrate and test the functionality of the `Stock` class.
     * Initializes a sample stock map and prints its contents.
     * 
     * @param args Command-line arguments (not used in this implementation).
     */
    public static void main(String[] args) {
        // Sample list of NDCs for testing
        List<String> ndcList = List.of("12345-6789", "98765-4321", "11111-2222", "33333-4444");

        // Create an instance of the `Stock` class
        Stock stock = new Stock();

        // Initialize the stock map with the provided NDC list
        stock.initializeMapStock(ndcList);

        // Access the stock map using the getter method
        HashMap<String, Boolean> mapStock = stock.getMapStock();

        // Print the contents of the stock map
        System.out.println("\nContents of mapStock:");
        for (Map.Entry<String, Boolean> entry : mapStock.entrySet()) {
            System.out.println("NDC: " + entry.getKey() + " - Value: " + entry.getValue());
        }
    }
}
