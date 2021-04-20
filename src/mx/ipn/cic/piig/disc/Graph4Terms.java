/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import java.util.ArrayList;
import java.util.Random;
import mx.ipn.cic.piig.disc.algoritmos.DISC;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

/**
 *
 * @author rolando
 */
public class Graph4Terms {
    public static String STR_NODE_DEPTH = "_StrNodeDepth_";
    public static boolean animate = false;
    public static int sleepTime = 5;
    
    public String name;
    public Graph graph;
    public ArrayList<String> terms;
    
    public static int max(int a, int b) {return a>b?a:b;}
    
    public String toString() {
        return graph.getId() +"( "+ graph.getNodeCount() +" nodos, "+ graph.getEdgeCount() +" aristas)";
    }
    
    /**
     * Clona el contenido de un nodo, si es que no existe aún en el grafo actual.
     * @param n Nodo a clonar.
     * @return El nodo clonado, o la referencia al nodo existente.
     */
    public Node cloneNode( Node n ) {
        Node a = graph.getNode(n.getId());
        if( a == null ) {
            a = graph.addNode(n.getId());
        }
        for( String att : n.getAttributeKeySet()) {
            a.setAttribute(att, (Object)n.getAttribute(att));
        }
        return a;
    }
    
    /**
     * Mezcla el grafo con otro dado.
     */
    public void merge( Graph4Terms other ) {
        terms.addAll(other.terms);
        graph.setStrict(false);
        
        for( Edge e : other.graph.getEachEdge()) {
            Edge a = graph.getEdge(e.getId());
            if( a == null ) {
                Node node0 = cloneNode(e.getNode0());
                Node node1 = cloneNode(e.getNode1());
                
                a = graph.addEdge(e.getId(), node0, node1, e.isDirected());
            }
            
            if( a != null ) for( String att : e.getAttributeKeySet() ) {
                a.setAttribute(att, (Object)e.getAttribute(att));
            }
        }
        format();
    }
    
    /**
     * Lee el grafo desde un archivo DGS.
     * @param name nombre del conjunto de elementos representado por el grafo.
     * @return el grafo que representa el conjunto de palabras.
     */
    public boolean readGraph(String name) {
        this.name = name;
        graph = new SingleGraph(name);
        
        try {
            graph.read(name + ".dgs");
            return true;
        } catch(Exception e) {
            this.graph = null;
            return false;
        }
    }
    
    /**
     * Despliega el grafo y le da formato.
     */
    public Viewer display() {
        Util.print(Util.PRINT_GENERAL,graph.getId() + ": "+ graph.getNodeCount() +" nodos, "+ graph.getEdgeCount() +" aristas.\n");
        graph.addAttribute("ui.antialias");
        Viewer v = graph.display();
        format();
        
        return v;
    }
    
    /**
     * Poda el grafo desechando los nodos que tienen menos de minDegree aristas,
     * se ejecuta iterativamente hasta que ya no haya nodos que no cumplan las 
     * condiciones descritas, ya que cada vez que se quita un nodo se quitan las 
     * aristas conectadas a él, por lo que puede ocasionar que otros nodos no cumplan
     * las condiciones.
     * @param minDegree El grado mínimo que debe tener un grafo para no ser eliminado.
     * @param corpus se usa para dar formato al grafo, ya que depende de la fuente. Puede ser 
     * null si format es false.
     * @throws java.lang.Exception
     */
    public void prune(int minDegree, Corpus corpus) throws Exception {
        boolean seguir = true;
        
        Util.print(Util.PRINT_GENERAL, "Podando... ");
        Util.print(Util.PRINT_DETAIL, " Inicio(Nivel "+ minDegree +")...\nNodos: "+ graph.getNodeCount() +" Aristas: "+ graph.getEdgeCount());
        
        if( animate ) display();
        while(seguir) {
            seguir = false;
            int i=0;
            for( Node n:graph ) {
                if( !terms.contains(n.getId())) {
                    if( n.getDegree() < minDegree) {
                        graph.removeNode(n);
                        seguir = true;
                        if(animate) Thread.sleep(sleepTime);
                    }
                }
                i++;
            }
            if(animate) format();
            Util.println(Util.PRINT_VERY_DETAIL, "Nodos: "+ graph.getNodeCount() +" Aristas: "+ graph.getEdgeCount());
        }
        
        Util.print(Util.PRINT_DETAIL, " Final(Nivel "+ minDegree +")...\nNodos: "+ graph.getNodeCount() +" Aristas: "+ graph.getEdgeCount());
        
        format();
        Util.println(Util.PRINT_GENERAL, "Ok.");
    }
    /**
     * Crea el nombre de una arista dados dos nodos.
     * @param n1 nombre del nodo 1.
     * @param n2 nombre del nodo 2.
     * @return 
     */
    public static String createEdgeName(String n1, String n2 ) {
        return n1 +"->"+ n2;
    }
    public static void formatNode( Node n ) {
        n.setAttribute("ui.label", n.getId());
        n.setAttribute("ui.style", "text-size:10;");
        n.setAttribute("ui.style", "text-color:rgb(100,100,100);");
        n.setAttribute("ui.style", "size:6px; fill-color:white; ");
    } 
    /**
     * Expande el grafo añadiendo el 1-vecindario de cada uno de los nodos del grafo.
     * @param corpus fuente de expansión.
     * @param pExp probabilidad de expandir el nodo.
     */
    public void expand(Corpus corpus, double pExp) {
        if( pExp > 1) {
            expand(corpus, (int) pExp);
        }
        
        Util.print(Util.PRINT_GENERAL, "Expandiendo con probabilidad "+ pExp +" ...");
        
        ArrayList<Node> nodos = new ArrayList<>();
        for( Node n : graph ) { nodos.add(n); }
        
        graph.setStrict(false);
        for( Node n : nodos ) {            
            Util.print(Util.PRINT_DETAIL, ".");
            
            Util.println(Util.PRINT_VERY_DETAIL, "Procesando: "+ n.getId() );
            ArrayList<ArrayList<String>> rels = corpus.getRelationships(n.getId());
            
            if( rels != null ) {
                for( int i=0; i<rels.size(); i++ ) {
                    ArrayList<String> r = rels.get(i);
                    for( String elem : r ) {
                        if( Math.random() > pExp ) continue;
                        formatNode(graph.addNode(elem));
                        Edge e = graph.addEdge(createEdgeName(n.getId(), elem) , n.getId(), elem, true );
                        if( e!= null) e.setAttribute(DISC.STR_TIPO, i);
                    }
                }
            }
        }
        
        Util.println(Util.PRINT_GENERAL, "Ok.");
    }
    /**
     * Expande el grafo añadiendo el 1-vecindario de cada uno de los nodos del grafo.
     * @param corpus fuente de expansión.
     * @param max máximo número de elementos a expandir por cdad nodos
     */
    public void expand(Corpus corpus, int max) {
        Util.print(Util.PRINT_GENERAL, "Expandiendo con máximo "+ max +" ...");
        
        ArrayList<Node> nodos = new ArrayList<>();
        for( Node n : graph ) { nodos.add(n); }
        
        graph.setStrict(false);
        for( Node n : nodos ) {            
            Util.print(Util.PRINT_DETAIL, ".");
            
            Util.println(Util.PRINT_VERY_DETAIL, "Procesando: "+ n.getId() );
            ArrayList<ArrayList<String>> rels = corpus.getRelationships(n.getId());
            
            if( rels != null ) {
                for( int i=0; i<rels.size(); i++ ) {
                    ArrayList<String> r = rels.get(i);
                    Random rnd = new Random();
                    for( int j=0; j<max && r.size()>=max; j++ ) {
                        String elem = r.get(rnd.nextInt(r.size()-1));
                        graph.addNode(elem);
                        Edge e = graph.addEdge(createEdgeName(n.getId(), elem) , n.getId(), elem, true );
                        if( e!= null) e.setAttribute(DISC.STR_TIPO, i);
                    }
                }
            }
        }
        
        Util.println(Util.PRINT_GENERAL, "Ok.");
    }
    public void format() {
        String termTextColor = "rgb( 120, 50,50);";
        String termFillColor = "rgb(250,250,250);";
        String termStrokeColor = termTextColor;
        
        for( Node n : graph ) {
            n.setAttribute("ui.label", n.getId());
            n.setAttribute("ui.style", "text-size:10;");
            n.setAttribute("ui.style", "text-color:rgb(100,100,100);");
            n.setAttribute("ui.style", "size:6px; fill-color:white; ");
        }
        
        for( String w : terms ) {
            Node n = graph.getNode(w);
            if( n==null ) continue;
            n.setAttribute("ui.style", "text-size:25;");
            n.setAttribute("ui.style", "text-color:"+ termTextColor);
            n.setAttribute("ui.style", "fill-color:"+ termFillColor);
            n.setAttribute("ui.style", "stroke-mode:plain;");
            n.setAttribute("ui.style", "stroke-color:"+ termStrokeColor);
            n.setAttribute("ui.style", "shape:rounded-box;");
            n.setAttribute("ui.style", "size-mode:fit;");
        }
    }
    /**
     * Da formato al grafo.
     */
    public void formatOld() {
        String termTextColor = "rgb( 120, 50,50);";
        String termFillColor = "rgb(250,250,250);";
        String termStrokeColor = termTextColor;
        String []col = {"black", "gray", "yellow", "orange", "blue", "cyan", "red", "pink", "magenta"};
        int R[] = { 0, 200, 245, 245,  66,  66, 245, 245, 245 };
        int G[] = { 0, 200, 245, 145,  66, 245,  66, 145,  66 };
        int B[] = { 0, 200,  66,  66, 245, 245,  66, 245, 245 };
        
        double max_degree = 0;
        for( Node n : graph ) {
            if( n.getDegree() > max_degree ) 
                max_degree = n.getDegree();
        }
        
        for( Node n : graph ) {
            int degree = n.getDegree();
            double pct = degree / max_degree;
            degree = (int) (50*pct + 10);
            int color = (int) (128 - 120*pct);
            
            n.setAttribute("ui.label", n.getId());
            n.setAttribute("ui.style", "text-size:"+ degree +";");
            n.setAttribute("ui.style", "text-color:rgb("+ color +","+ color +","+ color +");");
            n.setAttribute("ui.style", "size:6px; fill-color:white; ");
        }
        
        for( String w : terms ) {
            Node n = graph.getNode(w);
            if( n==null ) continue;
            n.setAttribute("ui.style", "text-size:25;");
            n.setAttribute("ui.style", "text-color:"+ termTextColor);
            n.setAttribute("ui.style", "fill-color:"+ termFillColor);
            n.setAttribute("ui.style", "stroke-mode:plain;");
            n.setAttribute("ui.style", "stroke-color:"+ termStrokeColor);
            n.setAttribute("ui.style", "shape:rounded-box;");
            n.setAttribute("ui.style", "size-mode:fit;");
        }
        
        for( Edge e : graph.getEachEdge() ) {
            int grado = max( e.getNode0().getDegree(), e.getNode1().getDegree() );
            double pct = 1 - grado / max_degree;
            
            int type = e.getAttribute(DISC.STR_TIPO);
            
            if( type < 0 ) {
                e.setAttribute("ui.style", "fill-color:gray;");
                e.setAttribute("ui.style", "text-color:gray;");
            } else {
                int r = (int) (R[type] + pct * (255 - R[type]));
                int g = (int) (G[type] + pct * (255 - G[type]));
                int b = (int) (B[type] + pct * (255 - B[type]));
                e.setAttribute("ui.style", "fill-color:rgb("+ r + ","+ g +","+ b +");");
                e.setAttribute("ui.style", "text-color:rgb("+ r + ","+ g +","+ b +");");
            }
            if( e.getAttribute(DISC.STR_PESO) != null ) {
                double w = e.getAttribute(DISC.STR_PESO);
                e.setAttribute("ui.label", String.format("%4.2f", w));
            }
        }
    }
}
