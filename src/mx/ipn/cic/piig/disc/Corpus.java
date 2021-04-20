/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import java.util.*;
import mx.ipn.cic.piig.disc.algoritmos.ConnectedSubgraphs;
import mx.ipn.cic.piig.disc.algoritmos.DISC;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;

/**
 * Clase abstracta para un corpus de términos.
 * @author rolando
 */
public abstract class Corpus {
    public static final int HYPERNYMS          = 0;
    public static final int HYPONYMS           = 1;
    public static final int INSTANCE_HYPERNYMS = 2;
    public static final int INSTANCE_HYPONYMS  = 3;
    public static final int MEMBER_HOLONYMS    = 4;
    public static final int MEMBER_MERONYMS    = 5;
    public static final int PART_HOLONYMS      = 6;
    public static final int PART_MERONYMS      = 7;
    public static final int WIKI_LINK          = 8;
    
    // 20200312
    // Constantes para regreso prematuro en la generación del grafo.
    public static final boolean EARLY_RETURN = true;
    public static final boolean LATE_RETURN = false;
    // Mostrar creación del grafo
    public static final boolean SHOW_GRAPH = true;
    public static final boolean HIDE_GRAPH = false;
    
    public static int NUM_RELATIONSHIPS  = 9;  
    public static int max(int a, int b) {return a>b?a:b;}

    /**
     * Constructor que solo llama a init() para preparar el corpus.
     * @throws Exception 
     */
    public Corpus() throws Exception {
        init();
    }
    
    /**
     * Genera un grafo tomando como semilla los términos dados en el parámetro terms.
     * @param name Nombre del grafo
     * @param terms Arreglo con los términos semilla.
     * @return El grafo generado
     */
    public Graph4Terms toGraph(String name, ArrayList<String> terms) {
        Graph4Terms retVal = new Graph4Terms();
        retVal.name = name;
        retVal.graph = new SingleGraph(name);
        retVal.terms = terms;
        
        retVal.graph.setStrict(false);
        
//        retVal.display();
      
        for(String term : terms) {
            toGraph(retVal, null, term, 0);
        }
        
        return retVal;
    }
    
    /**
     * Genera un grafo tomando como semilla los términos dados en el parámetro terms.
     * @param name Nombre del grafo
     * @param terms Arreglo con los términos semilla.
     * @param maxDepth Profundidad máxima de búsqueda
     * @return 
     */
    public Graph4Terms toGraph(String name, ArrayList<String> terms, int maxDepth) {
        Graph4Terms retVal = new Graph4Terms();
        retVal.name = name;
        retVal.graph = new SingleGraph(name);
        retVal.terms = terms;
        
        retVal.graph.setStrict(false);
 
        for(String term : terms) {
            toGraph(retVal, null, term, 0, 0, maxDepth);
        }
                
        return retVal;
    }
    
    /**
     * Genera un grafo buscando en profundidad conectar dos términos (source -> target) 
     * pasando por un término intermedio (midterm)
     * @param source        término fuente
     * @param target        término de destino
     * @param midTerm       término intermedio
     * @param earlyReturn   indica si se debe terminar la búsqueda cuando se conecta 
     *                      un término con el resto del grafo, aunque no sea el término 
     *                      que se está buscando
     * @param show          indica si se debe mostrar el grafo durante la búsqueda
     * @return el subgrafo del corpus que contecta source con target
     */
    public Graph4Terms toGraphDFS(String source, String target, String midTerm, boolean earlyReturn, boolean show) {
        Graph4Terms retVal = new Graph4Terms();
        
        retVal.terms = new ArrayList<>();
        retVal.terms.add(source);
        retVal.terms.add(target);
        retVal.name = "DFS";
        retVal.graph = new SingleGraph("DFS");
        retVal.graph.setStrict(false);
        
        retVal.graph.addNode(source);
        if( show ) retVal.display();
        
        toGraphDFS(1, retVal, source, midTerm, 50, false);
        retVal.graph.addNode(target);
        retVal.format();
        toGraphDFS(1, retVal, target, midTerm, 50, earlyReturn);

        return retVal;
    }
    
    /**
     * Da formato al nodo
     * @param n nodo a formatear
     */
    public static void formatNode( Node n ) {
        n.setAttribute("ui.label", n.getId());
        n.setAttribute("ui.style", "text-size:10;");
        n.setAttribute("ui.style", "text-color:rgb(100,100,100);");
        n.setAttribute("ui.style", "size:6px; fill-color:white; ");
    } 
    
    /**
     * Parte recursiva de la búsqueda en profundidad, trata de conectar source con
     * target
     * @param depth             profundidad actual
     * @param g                 grafo de búsqueda
     * @param source            término fuente
     * @param target            término de destino
     * @param maxDepth          máxima profundidad de búsqueda
     * @param retWhenConnect    indica si se debe terminar la búsqueda cuando se conecta 
     *                          un término con el resto del grafo, aunque no sea el término 
     *                          que se está buscando
     * @return 
     */
    public boolean toGraphDFS(int depth, Graph4Terms g, String source, String target, int maxDepth, boolean retWhenConnect) {
//        for( int i=0; i<depth; i++) Util.print(Util.PRINT_DETAIL, "\t");
        Util.print(Util.PRINT_DETAIL, "Nivel "+ depth + ") Procesando '"+ source +"'. ");
        ArrayList<ArrayList<String>> rels = getRelationships(source);

        // Si no encuentra relaciones, entonces el termino no existe. Regresar
        if( rels == null ) {
           Util.print(Util.PRINT_DETAIL, "No existe en el corpus.\n");
           return false;
        }
        
        // Para cada tipo de relación
        for( int i=0; i<rels.size(); i++ ) {
            ArrayList<String> r = rels.get(i);
            if(r.size() > 0) Util.print(Util.PRINT_DETAIL, "Probando "+ r.size() +" nodos. \n");
            else continue;
            
            // Existe el nodo de destino? Si, agragar la arista y regresar verdadero.
            if( r.contains(target) ) {
                g.graph.addNode(target);
                Edge e = g.graph.addEdge(Graph4Terms.createEdgeName(source, target), source, target);
                e.addAttribute(DISC.STR_TIPO, i);
                return true;
            }
                
            if( depth >= maxDepth-1 )
                return false;
            
            // Búsqueda en profundidad del nodo destino.
            for( String elem : r ) {
                try {
                    if( g.graph.getNode(elem) != null ) {
                        // Si el elemento ya existe en el grafo, solo conectar y continuar con otros elementos
                        Edge e = g.graph.addEdge(Graph4Terms.createEdgeName(source, elem), source, elem);
                        e.addAttribute(DISC.STR_TIPO, i);
                        if( retWhenConnect ) return true;
                    } else {
                        // si no entonces agregar nodo al grafo y llamar recursivamente a la búsqueda en profundidad
                        formatNode(g.graph.addNode(elem));
                        Edge e = g.graph.addEdge(Graph4Terms.createEdgeName(source, elem), source, elem);
                        e.addAttribute(DISC.STR_TIPO, i);
                        if( depth < maxDepth && toGraphDFS(depth+1, g, elem, target, maxDepth, retWhenConnect)) {
                            return true;
                        }
                    }
                } catch( Exception e) {} 
            }
        }

        return false;
    }
    
    /**
     * Función recursiva para construir un grafo a partir de un elemento del corpus.
     * En esta versión no se incrementa la profundidad de la búsqueda, por lo que es 
     * posible que nunca termine.
     * @param g4t
     * @param source elemento de origen
     * @param term elemento relacionado
     * @param type tipo de la relación entre from y to
     * @return true si la busqueda resulta en un grafo conectado.
     */
    public boolean toGraph(Graph4Terms g4t, String source, String term, int type) { 
        if( g4t.graph.getNodeCount() == 0) {
            g4t.graph.addNode(term);
            
            Util.print(Util.PRINT_DETAIL, "Procesando '"+ term +"'. ");
            ArrayList<ArrayList<String>> rels = getRelationships(term);
            
            if( rels == null ) {
                Util.print(Util.PRINT_DETAIL, "No existe en el corpus.\n");
            } else {                
                for( int i=0; i<rels.size(); i++ ) {
                    ArrayList<String> r = rels.get(i);
                    if(r.size() > 0) Util.print(Util.PRINT_DETAIL, "Probando "+ r.size() +" nodos. \n");
//                    r.sort((String r1, String r2) -> { return getCard(r1)-getCard(r2); });
                    for( String elem : r ) {
                        if( g4t.graph.getNode(elem) == null ) 
                            g4t.graph.addNode(elem);
                        
                        Edge e = g4t.graph.addEdge(Graph4Terms.createEdgeName(term, elem), term, elem);
                        e.addAttribute(DISC.STR_TIPO, i);
                    }
                }
                Util.print(Util.PRINT_DETAIL, "\n");
            }
            
            return true;
        }
        
        boolean termExists = g4t.graph.getNode(term) != null;
        
        /**
         * Si existe un nodo de origen se debe agregar una nueva arista desde el nodo
         * de origen hacia el nodo destino; si el nodo de destino ya existe, entonces
         * se marca para que no se procese doble.
         */
        if(source != null) {
            try {
                if( g4t.graph.getNode(source) == null ) {
                    Node n = g4t.graph.addNode(source);
                }
                if( g4t.graph.getNode(term) == null ) {
                    Node n = g4t.graph.addNode(term);
                } else {
                    termExists = true;
                }
                Edge e = g4t.graph.addEdge( Graph4Terms.createEdgeName(source, term), source, term, true); 
                e.addAttribute(DISC.STR_TIPO, type);
                g4t.format();
            } catch( IdAlreadyInUseException | ElementNotFoundException e) {}
        }
                
        /**
         * Si el nodo de destino no ha sido procesado, entonces se buscan sus conexiones
         * y se llama recursivamente a la función siempre y cuando no se haya exedido
         * la profundidad máxima.
         */
        if( !termExists ) {
            Util.print(Util.PRINT_DETAIL, "Procesando '"+ term +"'. ");
            ArrayList<ArrayList<String>> rels = getRelationships(term);
            if( rels == null ) {
                Util.print(Util.PRINT_DETAIL, "No existe en el corpus.\n");
            } else {                
                for( int i=0; i<rels.size(); i++ ) {
                    ArrayList<String> r = rels.get(i);
                    if(r.size() > 0) Util.print(Util.PRINT_DETAIL, "Probando "+ r.size() +" nodos. \n");
//                    r.sort((String r1, String r2) -> { return getCard(r1)-getCard(r2); });
                    for( String elem : r ) {
                        if( toGraph(g4t, term, elem, i) ) {
                            Util.print(Util.PRINT_DETAIL, "YA SE CONECTO!\n");
                            return true;
                        }
                    }
                }
                Util.print(Util.PRINT_DETAIL, "\n");
            }
        } else {
            Util.print(Util.PRINT_DETAIL, "Saltando '"+ term +"'\n");
        }
        
        ConnectedSubgraphs cs = new ConnectedSubgraphs();
        cs.init(g4t.graph);
        cs.compute();
        
        if( cs.getSubgraphCount() < 2 ) 
            return true;
        
        return false;
    }
    
    /**
     * Función recursiva para construir un grafo a partir de un elemento del corpus.
     * @param g4t
     * @param from elemento de origen
     * @param to elemento relacionado
     * @param depth profundidad de la búsqueda
     * @param type tipo de la relación entre from y to
     * @param maxDepth máxima profundidad de la recursividad
     */
    public void toGraph(Graph4Terms g4t, String from, String to, int type, int depth, int maxDepth) { 
        boolean toExists = g4t.graph.getNode(to) != null;
        
        /**
         * Si existe un nodo de origen se debe agregar una nueva arista desde el nodo
         * de origen hacia el nodo destino; si el nodo de destino ya existe, entonces
         * se marca para que no se procese doble.
         */
        if(from != null) {
            try {
                if( g4t.graph.getNode(from) == null ) {
                    Node n = g4t.graph.addNode(from);
                    n.addAttribute(Graph4Terms.STR_NODE_DEPTH, depth-1);
                }
                if( g4t.graph.getNode(to) == null ) {
                    Node n = g4t.graph.addNode(to);
                    n.addAttribute(Graph4Terms.STR_NODE_DEPTH, depth);
                } else {
                    toExists = true;
                }
                Edge e = g4t.graph.addEdge(from+"->"+to, from, to, true); 
                e.addAttribute(DISC.STR_TIPO, type);
            } catch( IdAlreadyInUseException | ElementNotFoundException e) {}
        }
        
        /**
         * Si el nodo de destino no ha sido procesado, entonces se buscan sus conexiones
         * y se llama recursivamente a la función siempre y cuando no se haya exedido
         * la profundidad máxima.
         */
        for(int i=0; i<depth; i++) Util.print(Util.PRINT_DETAIL, "    ");
        if( depth < maxDepth && !toExists ) {
            Util.print(Util.PRINT_DETAIL, "Procesando '"+ to +"'. ");
            ArrayList<ArrayList<String>> rels = getRelationships(to);
            if( rels == null ) {
                Util.print(Util.PRINT_DETAIL, "No existe en el corpus.\n");
            } else {                
                for( int i=0; i<rels.size(); i++ ) {
                    ArrayList<String> r = rels.get(i);
                    if(r.size() > 0) Util.print(Util.PRINT_DETAIL,"Añadiendo "+ r.size() +" nodos. \n");
                    for( String elem : r ) {
                        toGraph(g4t, to, elem, i, depth+1, maxDepth);
                    }
                }
                Util.print(Util.PRINT_DETAIL, "\n");
            }
        } else {
            Util.print(Util.PRINT_DETAIL, "Saltando '"+ to +"'.\n");
        }
    }
    /**
     * Contar el número de relaciones que tiene un elemento.
     * @param elem elemento a contar el número de relaciones.
     * @return  el número de relaciones que tiene el elemento.
     */            
    public abstract int getCard(String elem);
    /**
     * Verificar si el elemento se encuentra en el corpus.
     * @param elem elemento a buscar.
     * @return el número de apariciones del elemento en el corpus.
     */
    public abstract int checkElem(String elem);
    /**
     * Obtiene un arreglo de objetos relacionados con elem por cada tipo de relacion
     * que exista.
     * @param elem elemento de origen.
     * @return Arreglo con las páginas a las que se tiene link.
     */
    protected abstract ArrayList<ArrayList<String>> getRelationships(String elem);
    /**
     * Inicializa lo necesario para leer el corpus.
     * @throws Exception 
     */
    public abstract void init() throws Exception;
}
