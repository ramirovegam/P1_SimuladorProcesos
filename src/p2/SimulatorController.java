package p2;

import p2.PageReplacementAlgorithm;
import p2.SimulationStep;
import java.util.*;

public class SimulatorController {

    public List<SimulationStep> runSimulation(List<Integer> pages, PageReplacementAlgorithm algorithm) {
        List<SimulationStep> steps = new ArrayList<>();

        for (Integer page : pages) {
            int beforeFaults = algorithm.getPageFaults();

            // Procesar la página
            algorithm.processPage(page);
            boolean faultOccurred = algorithm.getPageFaults() > beforeFaults;

            // Obtener índice reemplazado según el algoritmo
            Integer replacedIndex = null;
            if (algorithm instanceof p2.FIFOAlgorithm) {
                replacedIndex = ((p2.FIFOAlgorithm) algorithm).getLastReplacedIndex();
            } else if (algorithm instanceof p2.OptimalAlgorithm) {
                replacedIndex = ((p2.OptimalAlgorithm) algorithm).getLastReplacedIndex();
            } else if (algorithm instanceof p2.LRUAlgorithm) {
            replacedIndex = ((p2.LRUAlgorithm) algorithm).getLastReplacedIndex();
            }else if (algorithm instanceof p2.ClockAlgorithm) {
            replacedIndex = ((p2.ClockAlgorithm) algorithm).getLastReplacedIndex();
           }else if (algorithm instanceof p2.VMINAlgorithm) {
            replacedIndex = ((p2.VMINAlgorithm) algorithm).getLastReplacedIndex();
            }else if (algorithm instanceof p2.WorkingSetAlgorithm) {
            replacedIndex = ((p2.WorkingSetAlgorithm) algorithm).getLastReplacedIndex();
            }else if (algorithm instanceof p2.PFFAlgorithm) {
            replacedIndex = ((p2.PFFAlgorithm) algorithm).getLastReplacedIndex();
            }else if (algorithm instanceof p2.SecondChanceAlgorithm) {
            replacedIndex = ((p2.SecondChanceAlgorithm) algorithm).getLastReplacedIndex();
            
}

            // Crear el paso de simulación
            steps.add(new SimulationStep(page, algorithm.getMemoryState(), faultOccurred, replacedIndex));
        }

        return steps;
    }
}
