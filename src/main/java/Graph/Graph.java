package Graph;

import Graph.Exceptions.GraphException;
import Graph.Exceptions.UncheckedException;
import Graph.Interfaces.IGraph;
import Parser.Interfaces.IVertexCtor;
import fj.data.*;
import lombok.*;

import java.util.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Graph<V extends Vertex, E extends Edge<V>> implements IGraph<V, E> {
    @Getter @Setter
    private String name;
    @Getter
    private Set<V> vertices;
    @Getter
    private Set<E> forwardEdges;
    @Getter
    private List<Object> order;
    @Getter
    private Map<Integer, V> verticesMap;

    private HashMap<V, fj.data.List<E>> inwardMap;
    private HashMap<V, fj.data.List<E>> outwardMap;
    private HashMap<V, fj.data.List<V>> parentMap;
    private HashMap<V, fj.data.List<V>> childrenMap;

    public Graph() {
        this.vertices = new HashSet<>();
        this.forwardEdges = new HashSet<>();
        this.order = new LinkedList<>();
        this.verticesMap = new HashMap<>();
        this.inwardMap = new HashMap<>();
        this.outwardMap = new HashMap<>();
        this.parentMap = new HashMap<>();
        this.childrenMap = new HashMap<>();
    }

    @Override
    public void addVertex(@NonNull final V v) {
        if(v.getCost() != 0) {
            order.add(v);
        }
        this.vertices.add(v);
    }

    @Override
    public void addEdge(@NonNull final E e) throws GraphException {
        if(!vertices.contains(e.getFrom()) || !vertices.contains(e.getTo())) {
            throw new GraphException("Non existing vertex is being added to the graph." +
                    " Use ensureVertex() to to ensure it exists.");
        }
        this.forwardEdges.add(e);
        this.order.add(e);
    }

    @Override
    public fj.data.List<V> getChildrenVertices(V v) {
        return childrenMap.get(v);
    }

    @Override
    public fj.data.List<V> getParentVertices(V v) {
        return parentMap.get(v);
    }

    @Override
    public V getVertex(int index) {
        return verticesMap.get(index);
    }

    @Override
    public V getVertex(String id) {
        List<V> vs = vertices.stream().filter(i -> i.getId().equals(id)).collect(Collectors.toList());
        if(vs.size() != 1) {
            return null;
        }
        return vs.get(0);
    }

    @Override
    public fj.data.List<E> getInwardsEdges(V v) {
        return inwardMap.get(v);
    }

    @Override
    public fj.data.List<E> getOutwardsEdges(V v) {
        return outwardMap.get(v);
    }

    /**
     * Returns the list of vertices that point to the given Vertex.
     */
    private List<V> getForwardVertices(@NonNull final V v) {
        Set<E> es = getOutwardEdges_(v);
        return es.stream().map(e -> e.getTo()).collect(Collectors.toList());
    }

    private List<V> getForwardVertices(@NonNull final Set<E> es) {
        return es.stream().map(e -> e.getTo()).collect(Collectors.toList());
    }

    /**
     * Returns the list of vertices that the given Vertex points to.
     */
    private List<V> getReverseVertices(@NonNull final V v) {
        Set<E> es = getInwardEdges_(v);
        return es.stream().map(e -> e.getFrom()).collect(Collectors.toList());
    }

    private List<V> getReverseVertices(@NonNull final Set<E> es) {
        return es.stream().map(e -> e.getFrom()).collect(Collectors.toList());
    }

    private Set<E> getInwardEdges_(@NonNull final V v) {
        return forwardEdges.stream().filter(e -> e.getTo().equals(v)).collect(Collectors.toSet());
    }

    private Set<E> getOutwardEdges_(@NonNull final V v) {
        return forwardEdges.stream().filter(e -> e.getFrom().equals(v)).collect(Collectors.toSet());
    }

    /**
     * Returns the Vertex with the given ID. Asserts that only one vertex exists with that ID.
     */
    @Deprecated
    public V lookUpVertexById(final int id) {
        return this.verticesMap.get(id);
    }

    /**
     * Returns the Vertex with the given ID. Asserts that only one vertex exists with that ID.
     */
    @Deprecated
    public V lookUpVertexById(@NonNull final String id) {
        List<V> vs = vertices.stream().filter(i -> i.getId().equals(id)).collect(Collectors.toList());
        if(vs.size() != 1) {
            return null;
        }
        return vs.get(0);
    }

    /**
     * Ensure a Vertex with the id exists in the graph, if the vertex exists, return it,
     * otherwise, create a new one and return it.
     * @param id the id of the vertex
     * @param ctor the constructor of the vertex
     * @return the ensured vertex
     * @see IVertexCtor
     * @see Parser.VertexCtor
     */
    public V ensureVertex(@NonNull final String id, @NonNull final IVertexCtor<V> ctor) {
        V res = getVertex(id);
        if(res == null) {
            // Vertex does not exist yet
            V newVertex = ctor.makeVertex(id);
            this.addVertex(newVertex);
            return newVertex;
        }
        else return res;
    }

    public void scheduleVertex(@NonNull final V v,
                               final int processor,
                               final int startTime) throws GraphException {
        if(!vertices.contains(v)) {
            throw new GraphException("Attempting to schedule a non-existing vertex");
        }
        else {
            v.setProcessor(processor);
            v.setStartTime(startTime);
            this.vertices.add(v);
        }
    }

    public E getForwardEdge(@NonNull final V from,
                            @NonNull final V to) {
        for(E e : forwardEdges) {
            if(e.getFrom().equals(from) && e.getTo().equals(to)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String a = vertices.toString();
        String b = forwardEdges.toString();
        return a.concat(b);
    }

    /**
     * Finalise the graph and fill in necessary information.
     */
    public void finalise() {
        getVertices().forEach(v -> calculateBottomLevels(v, 0));
        assignIds();
        buildMaps();
        /* ~forwardEdges()
         * ~vertices()
         */
    }

    private void buildMaps() {
        getVertices().forEach(v -> {
            Set<E> inwards = getInwardEdges_(v);
            Set<E> outwards = getOutwardEdges_(v);
            this.inwardMap.put(v, fj.data.List.iterableList(inwards));
            this.outwardMap.put(v, fj.data.List.iterableList(outwards));
            this.parentMap.put(v, fj.data.List.iterableList(getReverseVertices(inwards)));
            this.childrenMap.put(v, fj.data.List.iterableList(getForwardVertices(outwards)));
        });
    }

    /**
     * Assign unique numeric id to the vertices
     */
    @Synchronized
    private void assignIds() {
        int i = 0;
        for(V v : getVertices()) {
            v.setAssignedId(i);
            this.verticesMap.put(i, v);
            i++;
        }
    }

    /**
     * Exhaustively and recursively computed the bottom level for all the vertices.
     * @param v the vertex to compute
     * @param level the current level
     */
    private void calculateBottomLevels(@NonNull final V v,
                                       final int level) {
        if(v.getBottomLevel() < level) {
            v.setBottomLevel(level);
        }
        else {
            getReverseVertices(v).forEach(
                    w -> calculateBottomLevels(w, level + v.getCost()));
        }
    }

}