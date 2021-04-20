/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc.algoritmos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.graphstream.algorithm.AStar;
import org.graphstream.algorithm.Algorithm;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Path;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author rolando
 */
public class FAPDE implements Algorithm {
    boolean directed = true;                                                    // El grafo es dirigido?
    String weightAttibute = DISC.STR_PESO;                                      // Nombre del atributo que da peso a las aristas
    Graph theGraph = null,                                                      // El grafo sobre el que se calculan las distancias
            invGraph = null;                                                    // El mismo grafo, pero con las aristas en sentido inverso          
    ArrayList<Node> seeds = null;                                               // Las semillas que servirán como base para aproximar las distancias
    HashMap<Node, Dijkstra> distToSeed;                                         // Conjunto de árboles de Dijkstra que dan la distancia hacia una semilla
    HashMap<Node, Dijkstra> distFromSeed;                                       // Conjunto de árboles de Dijkstra que dan la distancia desde una semilla

    public void setAtributoDePeso(String peso) { weightAttibute = peso; }
    
    @Override
    public void init(Graph graph) {
        theGraph = graph;
        invGraph = getInvertedGraph(graph);
        distToSeed = new HashMap<>();
        distFromSeed = new HashMap<>();

    }

    @Override
    public void compute() {
        seeds = computeSeeds();                                                 // Calcula las log2 semillas
        
        /**
         * Para cada semilla u calcular el árbol de Dijkstra hacia y desde u
         */
        for(Node u : seeds) {
            Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, weightAttibute);
            dijkstra.init(theGraph);
            dijkstra.setSource(u);
            dijkstra.compute();
            distFromSeed.put(u, dijkstra);
                        
            dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, weightAttibute);
            dijkstra.init(invGraph);
            dijkstra.setSource(invGraph.getNode(u.getId()));
            dijkstra.compute();
            distToSeed.put(u, dijkstra);
        }
    }
    
    /**
     * Calcula el grafo con las aristas invertidas
     * @param graph El grafo que se quiere invertir
     * @return El grafo graph con las aristas invertidas
     */
    protected Graph getInvertedGraph( Graph graph ) {
        Graph retVal = new SingleGraph(graph.getId()+"_inverded");
        retVal.setStrict(false);
        retVal.setAutoCreate(true);
        
        for( Edge e : graph.getEachEdge() ) {
            Node from = e.getNode0(), to = e.getNode1(); 
            
            retVal.addNode(from.getId());
            retVal.addNode(to.getId());
            
            Edge inv_e;
            inv_e = retVal.addEdge(e.getId()+"_inv", to.getId(), from.getId(), true);
            Double w = e.getAttribute(weightAttibute);
            inv_e.addAttribute(weightAttibute, w);
        }
        
        return retVal;
    }
    
    /**
     * Calcula una aproximación a la distancia d(u,v)
     * @param u Nodo de origen
     * @param v Nodo de destino
     * @return La apriximación de la distancia entre u y v
     */
    public double  getDistance(Node u, Node v) {
        return getDistanceLCA(u, v);
    }
    
    /**
     * Calcula una aproximación a la distancia d(u,v), utilizando semillas, el LCA
     * y búsqueda de atajos.
     * @param u Nodo de origen  
     * @param v Nodo de destino
     * @return La apriximación de la distancia entre u y v
     */
    public double getDistanceLCA_Shortcuts(Node u, Node v) {
        double retVal = Double.POSITIVE_INFINITY;                       
        
        /**
         * Si se trata de la distanca de un nodo a si mismo: d(u,u) = 0.
         */
        if( u.getId().compareTo(v.getId()) == 0) {
            return 0;
        }
        
        /**
         * Para cada semilla s calculamos el camino de u-s y el camino s-v, con esos caminos
         * se determina el camino u-v que pasa por el LCA de ambos caminos. Así, la 
         * aproximación a d(u,v) será dada por la longitud del camino u-lca-v.
         */
        for( Node n : seeds ) {
            // Obtenemos los caminos
            Path pUS = distToSeed.get(n).getPath(invGraph.getNode(u.getId()));
            Path pVS = distFromSeed.get(n).getPath(v);
            
            // Ahora los caminos en forma de lista
            List<Node> pUSList = pUS.getNodePath();
            List<Node> pVSList = pVS.getNodePath();
            
            /**
             * Buscamos en los caminos hasta donde coinciden, el LCA en principio es 0
             * porque al menos tienen un ancestro común que es la semilla n.
             */
            int lca = 0;
            for( int i=0; i<pUSList.size() && i<pVSList.size(); i++ ) {
                lca = i;
                if( pUSList.get(i).getId().compareTo(pVSList.get(i).getId()) != 0) {
                    lca = i-1;
                    break;
                }
            }
            
            /**
             * Una vez que conocemos el LCA, podemos unificar ambos caminos en uno solo
             * que sale de u, pasa por el LCA y llega a v.
             */
            int i=0,j=lca;
            boolean shortcut = false;
            ArrayList<Node> path = new ArrayList<>();
            for( i=pUSList.size()-1; i>lca && !shortcut; i-- ) {
                path.add(pUSList.get(i));
                // Explorar si el nodo actual se conecta con un nodo en el otro camino
                for( j=pVSList.size()-1; j>lca && !shortcut; j--) {
                    // Existe la arista que representa un atajo?
                    Edge e = theGraph.getEdge(pUSList.get(i) +"->"+ pVSList.get(j));
                    if( e != null ) {
                        shortcut = true;
                        for( ; j<pVSList.size(); j++ ) {
                            path.add(pVSList.get(j));
                        }
                    }
                }
            }
            for( j=lca ; !shortcut && j<pVSList.size(); j++) {
                path.add(pVSList.get(j));
            }

            if( path.size() < 2) {
                continue;
            }
            
            /**
             * Para determinar los atajos hay que determinar si alguno de los nodos de
             * un camino tiene una conexión con algún nodo del otro camino.
             */
            
            /**
             * Determinamos la longitud del camino.
             */
            double pathLen = 0.0;
            for( i=1; i<path.size(); i++ ) {
                Edge e = theGraph.getEdge(path.get(i-1) +"->"+ path.get(i));
                pathLen = pathLen + (Double)e.getAttribute(weightAttibute);
            }
            /**
             * Si la longitud del camino es la menor, entonces es el valor que buscamos.
             */
            retVal = pathLen < retVal ? pathLen : retVal;
        }
        
        return retVal;
    }
    
    /**
     * Calcula una aproximación a la distancia d(u,v), utilizando semillas y el LCA
     * @param u Nodo de origen  
     * @param v Nodo de destino
     * @return La apriximación de la distancia entre u y v
     */
    public double getDistanceLCA(Node u, Node v) {
        double retVal = Double.POSITIVE_INFINITY;                       
        
        /**
         * Si se trata de la distanca de un nodo a si mismo: d(u,u) = 0.
         */
        if( u.getId().compareTo(v.getId()) == 0) {
            return 0;
        }
        
        /**
         * Para cada semilla n calculamos el camino de u-n y el camino n-v, con esos caminos
         * se determina el camino u-v que pasa por el LCA de ambos caminos. Así, la 
         * aproximación a d(u,v) será dada por la longitud del camino u-lca-v.
         */
        for( Node n : seeds ) {
            // Obtenemos los caminos
            Path pUS = distToSeed.get(n).getPath(invGraph.getNode(u.getId()));
            Path pVS = distFromSeed.get(n).getPath(v);
            
            if( pUS.empty() || pVS.empty() )
                continue;
            
            // Ahora los caminos en forma de lista
//            List<Node> pUSList = pUS.getNodePath();
//            List<Node> pVSList = pVS.getNodePath();
            List<Edge> pUSEdges = pUS.getEdgePath();
            List<Edge> pVSEdges = pVS.getEdgePath();
            
            /**
             * Buscamos en los caminos hasta donde coinciden, el LCA en principio es 0
             * porque al menos tienen un ancestro común que es la semilla n.
             */
            int lca = 0;
            for( int i=0; i<pUSEdges.size() && i<pVSEdges.size(); i++ ) {
                Edge eUS = pUSEdges.get(i);
                Edge eVS = pVSEdges.get(i);
                
                lca = i+1;
                
                if( eUS.getNode0().getId().compareTo(eVS.getNode0().getId()) != 0 ||
                    eUS.getNode1().getId().compareTo(eVS.getNode1().getId()) != 0 ) {
                    lca = i;
                    break;
                }
            }
            
            double pathLen = 0;
            for( int i=lca; i<pUSEdges.size(); i++ ) {
                pathLen += (Double)pUSEdges.get(i).getAttribute(weightAttibute);
            }
            for( int i=lca; i<pVSEdges.size(); i++ ) {
                pathLen += (Double)pVSEdges.get(i).getAttribute(weightAttibute);
            }
            
//            for( int i=0; i<pUSList.size() && i<pVSList.size(); i++ ) {
//                if( pUSList.get(i).getId().compareTo(pVSList.get(i).getId()) != 0) {
//                    lca = i-1;
//                    break;
//                }
//            }
            
//            /**
//             * Una vez que conocemos el LCA, podemos unificar ambos caminos en uno solo
//             * que sale de u, pasa por el LCA y llega a v.
//             */
//            ArrayList<Node> path = new ArrayList<>();
//            for( int i=pUSList.size()-1; i>lca; i-- ) {
//                path.add(pUSList.get(i));
//            }
//            for( int i=lca; i<pVSList.size(); i++) {
//                path.add(pVSList.get(i));
//            }
//            
//            if( path.size() < 2) {
//                continue;
//            }
//            
//            /**
//             * Determinamos la longitud del camino.
//             */
//            double pathLen = 0.0;
//            for( int i=1; i<path.size(); i++ ) {
//                Edge e = theGraph.getEdge(path.get(i-1) +"->"+ path.get(i));
//                pathLen = pathLen + (Double)e.getAttribute(weightAttibute);
//            }
            /**
             * Si la longitud del camino es la menor, entonces es el valor que buscamos.
             */
            retVal = pathLen < retVal? pathLen : retVal;
        }
        
        return retVal;
    }
    
    /**
     * Calcula una aproximación a la distancia d(u,v), utilizando semillas
     * @param u Nodo de origen  
     * @param v Nodo de destino
     * @return La apriximación de la distancia entre u y v
     */
    public double getDistanceBasic(Node u, Node v) {
        double retVal = Double.POSITIVE_INFINITY;
        
        if( u.getId().compareTo(v.getId()) == 0) {
            return 0;
        }
        
        for( Node n : seeds ) {
            /**
             * Algoritmo estandard: Determinar d(u,seed) y d(seed,v), de tal forma 
             * que d(u,v)<=d(u,seed)+d(seed,v).
             */            
            Node u_i = invGraph.getNode(u.getId());
            double dus = distToSeed.get(n).getPathLength(u_i);
            double dsv = distFromSeed.get(n).getPathLength(v);
            double dist =  dus + dsv; 
            retVal = dist < retVal ? dist : retVal;   
        }
        
        return retVal;
    }
    
    public void setDirected( boolean directed ) { this.directed = directed; }
    public void setWeightAttributeName( String weight ) { this.weightAttibute = weight; }
    
    /**
     * Calcula el logaritmo base 2 de un entero.
     * @param x el número 
     * @return 
     */
    public static int log2( int x ) {
        int retVal = 0;
        
        while( x != 0 ) { 
            retVal++;
            x >>= 1;
        }
        
        return retVal;
    }
    
    /**
     * Calcula las log2(n) semillas para el algoritmo de aproximación, con base 
     * en el grado de los nodos.
     * @return 
     */
    protected ArrayList<Node> computeSeeds() {
        Collection<Node> nodes = theGraph.getNodeSet();
        ArrayList<Node> listOfNodes = new ArrayList<>(nodes);
        Collections.sort(listOfNodes, (Node o1, Node o2) -> ( o2.getDegree() - o1.getDegree() ));
        
        //int k = (int) (Math.log(theGraph.getNodeCount()) / Math.log(2));
        int k = log2( theGraph.getNodeCount() );
        
        ArrayList<Node> retVal = new ArrayList<>();
        for( int i=0; i<k; i++ ) {
            retVal.add( listOfNodes.get(i) );
        }
        
        return retVal;
    }
    
    public static void main( String args[] ) {
        int n = 1000000;
        System.out.println("log2("+ n +")="+ FAPDE.log2(n));
    }
    
}
