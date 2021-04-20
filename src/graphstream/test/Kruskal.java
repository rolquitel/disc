/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphstream.test;

import java.util.ArrayList;
import java.util.HashSet;
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
public class Kruskal implements Algorithm {
    Graph theGraph, MST;
    String weightParam = "weight";

    @Override
    public void init(Graph graph) {
        theGraph = graph;
        MST = new SingleGraph("Kruskal_MST");
    }

    /**
     * Algoritmo de Kruskal - Determina si la arista e crearía un ciclo en T, 
     * utiliza las tablas de cortes: si ambos nodos en e se encuentran en el
     * mismo corte, entonces se crea un ciclo.
     * @param e La arista que se prueba.
     * @return Verdadero, si agregar la arista e crea un ciclo. Falso, si no.
     */
    public boolean createCycle(Edge e) {
        return ((HashSet)e.getNode0().getAttribute("kruskal_cut")).contains(e.getNode1()) || ((HashSet)e.getNode1().getAttribute("kruskal_cut")).contains(e.getNode0());
    }
    
    /**
     * Algoritmo de Kruskal - Actualiza las tablas de cortes para el algoritmo
     * de Kruskal. Unifica las tablas de corte de los dos cortes que se unen
     * por la arista que se añade.
     * @param e La arista que se añade al árbol T.
     */
    public void updateKruskal(Edge e) {
        HashSet<Node> cutA = (HashSet<Node>)e.getNode0().getAttribute("kruskal_cut");
        HashSet<Node> cutB = (HashSet<Node>)e.getNode1().getAttribute("kruskal_cut");
        
        cutA.addAll(cutB);
        cutA.stream().forEach((n) -> { n.setAttribute("kruskal_cut", cutA); });
    }
    
    /**
     * Algoritmo de Kruskal - calcula el árbol de expansión mínima.
     */
    @Override
    public void compute() {
        // Comienza con T=Ø
        MST.setStrict(false);
        MST.setAutoCreate(true);
        
        // Ordenar las aristas acendentemente por su costo
        ArrayList<Edge> aristas = new ArrayList<>();
        aristas.addAll(theGraph.getEdgeSet());
        aristas.sort((Edge a, Edge b) -> {
            double wa = (double)a.getAttribute(weightParam);
            double wb = (double)b.getAttribute(weightParam);            
            return wa<wb?-1:wa>wb?1:0;
        });
        
        for( Node n : theGraph ) {
            HashSet<Node> cut = new HashSet<>();
            cut.add(n);
            n.setAttribute("kruskal_cut", cut);  
        }
        
        // Insertar la arista e en T a menos que se cree un ciclo
        double total = 0;
        for( Edge e:aristas ) {
            if( !createCycle(e) ) {
                MST.addEdge(e.getNode0().getId() +"->"+ e.getNode1().getId(), e.getNode0().getId(), e.getNode1().getId());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Kruskal.class.getName()).log(Level.SEVERE, null, ex);
                }
                updateKruskal(e);
                total += (double)e.getAttribute(weightParam);
            } 
        }
        System.out.println("Costo total Kruskal: "+ total);
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

        Kruskal kruskal = new Kruskal();
        kruskal.init(graph);
        kruskal.getMST().display();
        kruskal.compute();
        
        Prim prim = new Prim();
        prim.init(graph);
        prim.getMST().display();
        prim.compute();
    }
}
