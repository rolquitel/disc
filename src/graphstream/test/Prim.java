/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphstream.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graphstream.algorithm.Algorithm;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author rolando
 */
public class Prim implements Algorithm {
    Graph theGraph, MST;
    String weightParam = "weight";

    @Override
    public void init(Graph graph) {
        theGraph = graph;
        MST = new SingleGraph("Kruskal_MST");
    }
    
    /**
     * Algoritmo de Prim - Calcula el conjunto de corte del corte dado.
     * @param cut El corte del cual se quiere calcular el conjunto de corte.
     * @return El conjunto de corte del corte dado.
     */
    protected HashSet<Edge> cutSet(HashSet<Node> cut) {
        HashSet<Edge> retVal = new HashSet<>();
        
        for( Node n:cut ) {
            Iterator<Node> i = n.getNeighborNodeIterator();
            while( i.hasNext() ) {
                Node m = i.next();
                if( !cut.contains(m) ) {
                    Edge e = theGraph.getEdge(n.getId() +"-"+ m.getId());
                    if( e != null) retVal.add(e);
                }
            }
        }
        
        return retVal;
    }
    
    /**
     * Algoritmo de Prim - Calcula la siguiente arista a agregar a T.
     * @param cut El conjunto de nodos que ya han sido agregados a T.
     * @return  La siguiente arista a agregar.
     */
    protected Edge nextPrimEdge(HashSet<Node> cut) {
        Edge retVal = null;
        
        HashSet<Edge> cut_set = cutSet(cut);
        for( Edge e:cut_set ) {
            if( retVal == null ) {
                retVal = e;
            } else {
                if( (double)e.getAttribute(weightParam) < (double)retVal.getAttribute(weightParam) ) {
                    retVal = e;
                }
            }
        }
        
        if( cut.contains(retVal.getNode0()) ) cut.add(retVal.getNode1());
        else cut.add(retVal.getNode0());

        return retVal;
    }

    /**
     * Algoritmo de Prim - calcula el árbol de expansión mínima. Partiendo de
     * cualquier nodo como corte inicial, se va agragando la arista de menor 
     * costo en el conjunto de corte, haciendo crecer el corte.
     */
    @Override
    public void compute() {
        // Comienza con T=Ø
        MST.setStrict(false);
        MST.setAutoCreate(true);
        
        HashSet<Node> cut = new HashSet<>();        
        Node n = theGraph.getNodeIterator().next();
        
        MST.addNode(n.getId());
        cut.add(n);
                
        double total = 0;
        while( cut.size() < theGraph.getNodeSet().size() ) {
            Edge e = nextPrimEdge(cut);
            MST.addEdge(e.getNode0().getId() +"-"+ e.getNode1().getId(), e.getNode0().getId(), e.getNode1().getId());
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(Prim.class.getName()).log(Level.SEVERE, null, ex);
            }
            total += (double)e.getAttribute(weightParam);
        }
        System.out.println("Costo total Prim: "+ total);
    }
    
    public Graph getMST() { return MST; }
    public void setWeightParam(String wp) { weightParam = wp; }
    
    @SuppressWarnings("empty-statement")
    public static void main(String []args) {
        DorogovtsevMendesGenerator gen = new DorogovtsevMendesGenerator();
        Graph graph = new DefaultGraph("Kruskal Test");

        graph.display();

        gen.addEdgeAttribute("weight");
        gen.setEdgeAttributesRange(1, 100);
        gen.addSink(graph);
        gen.begin();
        for (int i = 0; i < 100 && gen.nextEvents(); i++);
        gen.end();
        
//        for( Edge e:graph.getEdgeSet()) { System.out.println(e.getId()); }

        Prim prim = new Prim();
        prim.init(graph);
        prim.getMST().display();
        prim.compute();
    }
}
