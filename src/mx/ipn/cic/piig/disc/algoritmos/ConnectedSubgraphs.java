/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc.algoritmos;

import java.util.ArrayList;
import org.graphstream.algorithm.*;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author rolando
 */
public class ConnectedSubgraphs implements Algorithm{
    public static String SUBGRAPH_ATT = "__SUBGRAPH__";
    Graph graph;
    ArrayList<Graph> subgraphs;

    @Override
    public void init(Graph graph) {
        this.graph = graph;
    }
    
//    @Override
//    public void compute() {
//        ConnectedComponents cc = new ConnectedComponents(graph);
//        cc.setCountAttribute(SUBGRAPH_ATT);
//        cc.compute();
//        
//        subgraphs = new ArrayList<>();
//        for(int i=0; i<cc.getConnectedComponentsCount(); i++) {
//            Graph g = new SingleGraph(graph.getId() +"_"+ i);
//            subgraphs.add(i, g);
//            g.setStrict(false);
//        }
//        
//        for( Edge e : graph.getEachEdge() ) {
//            Graph g = subgraphs.get(e.getNode0().getAttribute(SUBGRAPH_ATT));
//            Node a = g.addNode(e.getNode0().getId());
//            Node b = g.addNode(e.getNode1().getId());
//            Edge n = g.addEdge(e.getId(), a, b);
//            
//            if( n != null ) for( String att : e.getEachAttributeKey()) {
//                Object attVal = e.getAttribute(att);
//                n.addAttribute(att, attVal);
//            }
//        }
//    }

    @Override
    public void compute() {
        ConnectedComponents cc = new ConnectedComponents(graph);
        cc.setCountAttribute(SUBGRAPH_ATT);
        cc.compute();
        
        subgraphs = new ArrayList<>();
        for(int i=0; i<cc.getConnectedComponentsCount(); i++) {
            Graph g = Graphs.clone(graph);
            subgraphs.add(i, g);
            
            for( Node n : g ) {
                if( (int)n.getAttribute(SUBGRAPH_ATT) != i) {
                    g.removeNode(n);
                }
            }
        }
    }
    
    public ArrayList<Graph> getSubgraphs() {
        return subgraphs;
    }
    
    public int getSubgraphCount() {
        return subgraphs.size();
    }
}
