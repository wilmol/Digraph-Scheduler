package SolverOld;

import CommonInterface.ISearchState;
import Datastructure.FastPriorityBlockingQueue;
import Graph.EdgeWithCost;
import Graph.Graph;
import Graph.Vertex;
import fj.F;
import fj.F1Functions;
import lombok.Data;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@Data
public final class AStarSolverPar extends AbstractSolver {
    private final Queue<SearchState> queue;
    private Timer timer;
    public AStarSolverPar(Graph<Vertex, EdgeWithCost<Vertex>> graph, int processorCount) {
        super(graph, processorCount);
        queue= new FastPriorityBlockingQueue<>();
    }

    @Override
    public void doSolve() {
        SearchState.init(graph);


        if(updater != null) {
            /* We have an updater and a UI to update */
            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                                          @Override
                                          public void run() {
                                              updater.update(queue.peek());
                                          }
                                      },
                    100, 100);
        }

        queue.add(new SearchState());

        while(true) {
            SearchState s = queue.remove();
            System.err.println(s.getSize() + " " + s.getPriority() + " " + queue.size());
            if(s.getSize() == graph.getVertices().size()) {
                // We have found THE optimal solution
                if(updater != null && timer != null) {
                    updater.update(s);
                    timer.cancel();
                }
                scheduleVertices(s);
                return;
            }
            s.getLegalVertices().parallelStream().forEach( v -> {
                IntStream.of(0, processorCount-1).parallel().forEach( i -> {
                            SearchState next = new SearchState(s, v, i);
                            if(!queue.contains(next)) {
                                queue.add(next);
                            }
                        }
                );
            });
            /* Expansion */
        }
    }

    //@Override
    public ISearchState pollState() {
        return queue.peek();
    }

    /*
    OPEN ← emptyState
    while OPEN 6 = ∅ do s ← PopHead ( OPEN )
    if s is complete solution then return s as optimal solution
    Expand state s into children and compute f ( s child )
     for each OPEN ← new states
     */
}
