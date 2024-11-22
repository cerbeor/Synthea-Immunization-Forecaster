package org.mitre.synthea.codebase.generated.Stock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Stock {

    private HashMap<String, Boolean> mapStock; // Private due to encapsulation principles

    // Function that creates the stock map from the list of existing NDCs
    // by randomly assigning true or false
    public void initializeMapStock(List<String> ndcList) {
        System.out.println("Initializing the stock map with the following NDC list: " + ndcList);
        mapStock = new HashMap<>();
        Random random = new Random();
        int trueCount = 0;
        int falseCount = 0;

        for (String ndc : ndcList) {
            boolean value = random.nextBoolean();
            mapStock.put(ndc, value);
            if (value) {
                trueCount++;
            } else {
                falseCount++;
            }
        }

        int total = trueCount + falseCount;
        double truePercentage = (trueCount * 100.0) / total;
        double falsePercentage = (falseCount * 100.0) / total;

        System.out.println("Percentage of true: " + truePercentage + "%");
        System.out.println("Percentage of false: " + falsePercentage + "%");
    }
    // Getter method to access mapStock
    public HashMap<String, Boolean> getMapStock() {
        return mapStock;
    }

    public static void main(String[] args) {
    // Sample list of NDCs for testing
    List<String> ndcList = List.of("12345-6789", "98765-4321", "11111-2222", "33333-4444");

    Stock stock = new Stock();
    stock.initializeMapStock(ndcList);

    // Accessing mapStock using the getter method
    HashMap<String, Boolean> mapStock = stock.getMapStock();

    // Printing the contents of mapStock
    System.out.println("\nContents of mapStock:");
    for (Map.Entry<String, Boolean> entry : mapStock.entrySet()) {
        System.out.println("NDC: " + entry.getKey() + " - Value: " + entry.getValue());
        }
    }
}
