/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc.algoritmos;

import java.util.HashSet;
import org.graphstream.algorithm.Algorithm;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author rolando
 */
public class DijstraTree implements Algorithm {
    String weightAttibute = "weight";
    Graph tree;
    Graph graph;
    Node seed;
    
    void setSeed( String seed ) {
        this.seed = graph.getNode(seed);
    } 
    
    public Graph getTree() {
        return tree;
    }

    @Override
    public void init(Graph graph) {
        this.graph = graph;
    }

    @Override
    /**
     * Algoritmo de Dijkstra - calcula el árbol de caminos mínimos a partir de un nodo fuente
     */
    public void compute() {
        HashSet<Node> nodos = new HashSet<>();
        nodos.addAll(graph.getNodeSet());
        
        for( Node n : nodos ) {
            n.setAttribute("dijkstra_len_from_"+ seed.getId(), Double.POSITIVE_INFINITY);
            n.setAttribute("dijkstra_source_from_"+seed.getId(), n);
        }
        
        Node s = seed;
        s.setAttribute("dijkstra_len_from_"+seed.getId(), 0.0);
        tree = new SingleGraph("Dijkstra_from_"+ seed.getId());
        tree.setStrict(false);
        tree.setAutoCreate(true);
        tree.addNode(s.getId());
        nodos.remove(s);
        
        Node n = s;
        while( !nodos.isEmpty() ) {
            updateDijkstraData(n, seed);
            n = nextNode(nodos, seed);
            Edge nextEdge = n.getAttribute("dijkstra_source_from_"+seed.getId());
            double w = nextEdge.getAttribute(weightAttibute);
            Edge e = tree.addEdge(nextEdge.getId(), nextEdge.getNode0().getId(), nextEdge.getNode1().getId(), true);
            e.setAttribute(weightAttibute, w);
        }
    }
    
    /**
     * Algoritmo de Dijkstra - actualiza los valores de distancia para los nodos que están al alcance del siguiente nodo a añadir
     * @param n El siguiente nodo a añadir
     * @param seed El nodo fuente para el árbol de Dijkstra
     */
    protected void updateDijkstraData(Node n, Node seed) {
        for( int i = 0; i<n.getOutDegree(); i++ ) {
            Edge e = n.getLeavingEdge(i);
            double len = (Double)n.getAttribute("dijkstra_len_from_"+seed.getId()) + (Double)e.getAttribute(weightAttibute);
            if( len < (Double)e.getNode1().getAttribute("dijkstra_len_from_"+seed.getId())) {
                e.getNode1().setAttribute("dijkstra_len_from_"+seed.getId(), len);
                e.getNode1().setAttribute("dijkstra_source_from_"+seed.getId(), e);
            }
        }
    }
    
    /**
     * Algoritmo de Dijkstra - determina el siguiente nodo a añadir al árbol de caminos mínimos
     * @param not_visited Conjunto de nodos que no han sido añadidos al árbol
     * @param seed El nodo fuente para el árbol de Dijkstra
     * @return El siguiente nodo a añadir
     */
    protected Node nextNode(HashSet<Node> not_visited, Node seed) {
        Node retVal = null;
        
        for( Node n : not_visited ) {
            if( retVal == null || (Double)n.getAttribute("dijkstra_len_from_"+seed.getId()) < (Double)retVal.getAttribute("dijkstra_len_from_"+seed.getId()) ) {
                retVal = n;
            }
        }
        
        not_visited.remove(retVal);
        return retVal;
    }
    
}
