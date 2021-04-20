/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc.algoritmos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import mx.ipn.cic.piig.disc.Util;
import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.APSP.APSPInfo;
import org.graphstream.algorithm.Algorithm;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

/**
 * Clase para llevar el registro de ejecución del algoritmo principal.
 * @author rolando
 */
class Trace {
    ArrayList<String> columnNames;
    ArrayList<ArrayList> traceTable;
    ArrayList<Double> curRow;
    
    /**
     * Constructor.
     */
    public Trace() {
        columnNames = new ArrayList<>();
        traceTable = new ArrayList<>();
        curRow = new ArrayList<>();        
        traceTable.add(curRow);
    }
    
    /**
     * Crea una nueva entrada de registro.
     */
    public void newRow() {
        curRow = new ArrayList<>();
        traceTable.add(curRow);
    }
    
    /**
     * Agrega un valor de la traza.
     * @param s El valor a agregar.
     */
    public void add(Double s) {
        curRow.add(s);
    }
    
    /**
     * Agrega el nombre de una columna de la traza.
     * @param s Nombre de la columna.
     */
    public void addToHeader(String s) {
        columnNames.add(s);
    }
    
    /**
     * Convierte la traza a cadena.
     * @return La traza en forma de tabla.
     */
    @Override
    public String toString() {
        Formatter f = new Formatter(Locale.US);
        f.format("%s\n", columnNames.toString());
        for( ArrayList<Double> row:traceTable ) {
//            f.format("%s\n", row.toString() );
            f.format("[");
            row.forEach((d) -> {
                f.format("%8.6f, ", d);
            });
            f.format("]\n");
        }
        return f.toString();
    }
}

/**
 * Clase que implementa el algoritmo DIS-C.
 * 20190820: 
 *      Se corrigió el calculo de distancia utilizando Dijkstra.
 *      Se agrego DISC que al final tendrá la tabla de distancias entre
 *          todos los nodos. El orden de los nodos es el que se obtiene con 
 *          graph.getNodeSet().
 * @author rolando
 */
public class DISC implements Algorithm {
    public static double DEFAULT_EPSILON = 1e-5;
    public Graph graph, disc;
    double delta[], delta_inv[];
//    double[][] DISC;                                  // 20190820
//    ArrayList<Node> nodos;                            // 20190820
    Trace trace;                                      // 20190820
    
    /**
     * 20190902
     */
    public enum AlgoDeCalculoDeDistancias { USE_DIJKSTRA, USE_FLOYD, USE_FAPDE, USE_DIJKSTRA_MATRIX_CPU, USE_DIJKSTRA_MATRIX_GPU }
    protected AlgoDeCalculoDeDistancias algo = AlgoDeCalculoDeDistancias.USE_DIJKSTRA;
    public void setAlgoDeCalculoDeDistancias(AlgoDeCalculoDeDistancias a) { algo = a; }
    
    public static String STR_PESO = "weight";
    public static String STR_GENERALIDAD = "v_a";
    public static String STR_TIPO = "type";
    public static String STR_GRADO_DE_ENTRADA = "i_a";
    public static String STR_GRADO_DE_SALIDA = "o_a";
    public static String STR_COSTO_DE_ENTRADA = "wi_a";
    public static String STR_COSTO_DE_SALIDA = "wo_a";
    public static String STR_DISC_ATT = "disc_to_node_";
    /**************************************************************************/
    
    /**
     * Inicializa el algoritmo
     * @param graph El grafo de origen
     */
    @Override
    public void init(Graph graph) {
        this.graph = graph;
        trace = new Trace();
        
        /**
         * Agregado 20190820
         */
//        DISC = new double[graph.getNodeCount()][graph.getNodeCount()];
//        nodos = new ArrayList<>(graph.getNodeSet());
//        nodos.sort((Node a, Node b) -> {
//            return a.getId().compareTo(b.getId());
//        });
    }
    
    public static int max(int a, int b) {return a>b?a:b;}
    protected double sqr(double x) { return x*x; }
    protected double abs(double x) { return x<0?-x:x; }
    
    public void display() { 
        disc.addAttribute("ui.antialias"); 
        disc.display(); 
        formatGraph(null);
    }
    
    public void display(HashSet<String> nodos) {
        disc.addAttribute("ui.antialias"); 
        disc.display(); 
        formatGraph(nodos);
    }

    /**
     * Ejecuta el algoritmo DIS-C.
     */
    @Override
    public void compute() {
        Util.print(Util.PRINT_GENERAL, "Ejecutando DISC ");
        int tipos = 0;
        
        /**
         * Determina la cantidad de tipos diferentes de relaciones
         */
        for(Edge e : graph.getEdgeSet()) {
            Integer type = (Integer)e.getAttribute(STR_TIPO);
            if( type != null ) {
                tipos = tipos<type?type:tipos;
            }
        }
        tipos++;
        delta = new double[tipos];
        delta_inv = new double[tipos];
        for(int i=0; i<delta.length; i++) {
            delta[i]        = 1;
            delta_inv[i]    = 1;
        }
        
        /**
         * Calcula los atributos topológicos de cada nodo: 
         * + i_a - grado de entrada
         * + o_a - grado de salida
         * + w_ia - costo de entrar al nodo
         * + w_oa - costo de salir del nodo
         */
        for(Node n : graph.getNodeSet()) {
            double o_a = n.getOutDegree();
            double i_a = n.getInDegree();
            double w_ia = o_a / (i_a + o_a);
            double w_oa = i_a / (i_a + o_a);
            // 20190902
            n.addAttribute(STR_GRADO_DE_ENTRADA, i_a);
            n.addAttribute(STR_GRADO_DE_SALIDA, o_a);
            n.addAttribute(STR_COSTO_DE_ENTRADA, w_ia);
            n.addAttribute(STR_COSTO_DE_SALIDA, w_oa);
            n.addAttribute(STR_GENERALIDAD, 1.0);
        }
                
        /*
         * NUEVO
         */
        double sum_d[];                                 // Sumatoria de la relación directa
        double sum_d_i[];                               // Sumatoria de la relación inversa
        int card[];                                     // Número de aristas de cada tipo de relación

        double eps = 1000, curEPS = 1000, oldEPS=1000;  // Parametros de las condiciones de paro
        double eps_K = 1e-7;
        double p_w = 0.5;
        
        /**
         * Esta parte sirve para llevar el registro de una traza de la ejecución 
         */
        trace.addToHeader("$j$");
        for(Node n:graph ) {
            trace.addToHeader("$v_{"+ n.getId() +"}$");
        }
        for(int j=0; j<delta.length; j++) {
            trace.addToHeader("$\\delta^"+ j +"$");
            trace.addToHeader("$\\bar{\\delta}^"+ j +"$");
        }
        trace.addToHeader("$\\epsilon_j$"); 
                
        /**
         * Repetir hasta alcanzar el umbral de convergencia
         */       
        for (int j=0; j<15 && eps>eps_K; j++ ) {
            Util.print(Util.PRINT_GENERAL, ".");
            long tIni = System.currentTimeMillis();
            /**
             * V_\gamma \gets V
             * A_\gamma \gets \emptyset
             */
            disc = new SingleGraph("DISC");
            disc.setStrict(false);
            Util.println(Util.PRINT_DETAIL, j +") "+ new Date() );
            Util.println(Util.PRINT_DETAIL, j + ".1) añadiendo nodos ...");
            for(Node n : graph) {
                String name = n.getId();
                Node a = disc.addNode(name);
                
                a.addAttribute(STR_GRADO_DE_ENTRADA, (double)n.getAttribute(STR_GRADO_DE_ENTRADA));
                a.addAttribute(STR_GRADO_DE_SALIDA, (double)n.getAttribute(STR_GRADO_DE_SALIDA));
                a.addAttribute(STR_COSTO_DE_ENTRADA, (double)n.getAttribute(STR_COSTO_DE_ENTRADA));
                a.addAttribute(STR_COSTO_DE_SALIDA, (double)n.getAttribute(STR_COSTO_DE_SALIDA));
                a.addAttribute(STR_GENERALIDAD, (double)n.getAttribute(STR_GENERALIDAD));
                
                Edge e = disc.addEdge(name +"->"+ name, name, name, true); 
                e.addAttribute(STR_PESO, 0.0);
                e.addAttribute(STR_TIPO, -1);
            }

            sum_d = new double[delta.length];
            sum_d_i = new double[delta.length];
            card = new int[delta.length];

            /**
             * Para cada arista e, calcular los pesos conceptuales
             */
            Util.println(Util.PRINT_DETAIL, j+ ".2) calcular pesos conceptuales de aristas ...");
            for( Edge e : graph.getEdgeSet() ) {
                int type = (int)e.getAttribute(STR_TIPO);
                double w_ia = (double)(e.getNode0().getAttribute(STR_COSTO_DE_ENTRADA));
                double w_ib = (double)(e.getNode1().getAttribute(STR_COSTO_DE_ENTRADA));
                double w_oa = (double)(e.getNode0().getAttribute(STR_COSTO_DE_SALIDA));
                double w_ob = (double)(e.getNode1().getAttribute(STR_COSTO_DE_SALIDA));
                double v_a = (double)(e.getNode0().getAttribute(STR_GENERALIDAD));
                double v_b = (double)(e.getNode0().getAttribute(STR_GENERALIDAD));
                double d_rho = delta[type];
                double d_rho_inv = delta_inv[type];

                double w_ab = (p_w)*(v_a*w_oa + v_b*w_ib) + (1-p_w)*d_rho ;
                double w_ba = (p_w)*(v_b*w_ob + v_a*w_ia) + (1-p_w)*d_rho_inv;

                sum_d[type]   += w_ab;
                sum_d_i[type] += w_ba;
                card[type]++;
            }

            /**
             * Para cada relación \rho, calcular \delta^rho y \bar{delta}^rho
             */
            Util.println(Util.PRINT_DETAIL, j+ ".3) calcular pesos de cada tipo de relacion ...");
            for(int d=0; d<delta.length; d++) {
                delta[d] = sum_d[d]/card[d];
                delta_inv[d] = sum_d_i[d]/card[d];
            }
                        
            /**
             * Para cada arista e crear, en el grafo DIS-C, una arista en el
             * sentido normal de la relación y otra en el sentido inverso
             */
            Util.println(Util.PRINT_DETAIL, j+ ".4) añadiendo aristas ...");
            for( Edge e : graph.getEdgeSet() ) {
                String aName = e.getNode0().getId();
                String bName = e.getNode1().getId();
                int type = e.getAttribute("type");
                if( aName==null || bName==null ) {
                    aName = bName = "";
                }
                Edge ab = disc.addEdge(aName +"->"+ bName, aName, bName, true);
                    ab.addAttribute(STR_PESO, this.delta[type]);
                Edge ba = disc.addEdge(bName +"->"+ aName, bName, aName, true);
                    ba.addAttribute(STR_PESO, this.delta_inv[type]);
                ab.addAttribute(STR_TIPO, type);
                ba.addAttribute(STR_TIPO, type);
                
            }

            /**
             * Determinar las distancias conceptuales de cada nodo a todos los
             * demás y determina el cambio de una iteración a otra
             */
            Util.println(Util.PRINT_DETAIL, j+ ".5) calcular umbrales ...");
            curEPS = calcular();
            eps = sqr(oldEPS - curEPS);
            oldEPS = curEPS;
            
            /**
             * Añade los valores de la traza
             */
            Util.println(Util.PRINT_DETAIL, j+ ".6) crear una traza ...");
            trace.add(new Double(j));
            for(Node n:disc) {
                trace.add((Double)n.getAttribute(STR_GENERALIDAD));
            }
            for(int i=0; i<delta.length; i++) {
                trace.add(delta[i]);
                trace.add(delta_inv[i]);
            }
            trace.add(eps);
            trace.newRow();
            
            long tFin = System.currentTimeMillis();
            Util.println(Util.PRINT_DETAIL,String.format("eps=%g en %d ms\n\n", eps, tFin-tIni));
        }     
        
        /**
         * Copia los atributos del grafo original al grafo DIS-C
         */
        for( Node n:graph ) {
            Node m = disc.getNode(n.getId());
            for( String att : n.getAttributeKeySet() ) {
                m.setAttribute(att, (Object)n.getAttribute(att));
            }
        }
        
        Util.println(Util.PRINT_GENERAL, "Ok.");
    }
    
    /**
     * Obtiene la distancia conceptual de una palabra a otra
     * @param a Palabra de origen
     * @param b Palabra de destino
     * @return La distancia conceptual de la palabra a a la palabra b
     */
    public double getDISC(String a, String b) {
//        int from = buscarNodo(a);
//        int to   = buscarNodo(b);
//        
//        return DISC[from][to];
        return ((HashMap<String,Double>)disc.getNode(a).getAttribute(STR_DISC_ATT)).get(b);
    }
    
    /**
     * Calcula los valores de DIS-C entre todos los conceptos
     * @return El valor de la variación del total de los nodos
     */
    public double calcular() {
        switch(algo) {
            case USE_FLOYD: return calcularFloyd();
            case USE_FAPDE: return calcularFAPDE();
            case USE_DIJKSTRA_MATRIX_CPU: return calculaDijkstraMatrixCPU();
            case USE_DIJKSTRA_MATRIX_GPU: return calculaDijkstraMatrixGPU();
            default: return calcularDijkstra();
        }
    }
    
    /**
     * Calcula los valores de DIS-C entre todos los conceptos utilizando el 
     * algoritmo de Floyd para el cálculo de las distancias
     * @return El valor de la variación del total de los nodos
     */
    public double calcularFloyd() {
        double retVal = 0;
        APSP apsp = new APSP(disc);
        apsp.setDirected(false);
        apsp.setWeightAttributeName(STR_PESO);
        apsp.compute();
        
        for(Node nodeA : disc ) {
            double from = 0;
            double to   = 0;
            
            HashMap<String,Double> dist = new HashMap<>();
            nodeA.setAttribute(STR_DISC_ATT, dist);
            APSPInfo infA = nodeA.getAttribute(APSPInfo.ATTRIBUTE_NAME);
            for( Node nodeB : disc ) {
                dist.put(nodeB.getId(), infA.getLengthTo(nodeB.getId()));
                
                APSPInfo infB = nodeB.getAttribute(APSPInfo.ATTRIBUTE_NAME);
                from += infA.getLengthTo(nodeB.getId());
                to   += infB.getLengthTo(nodeA.getId());
            }
            
            double v_a = (double)(nodeA.getAttribute(STR_GENERALIDAD));
            retVal += sqr(v_a - generalidad(from, to));
            graph.getNode(nodeA.getId()).setAttribute(STR_GENERALIDAD, generalidad(from, to));
        }
        
        return retVal / disc.getNodeCount();
    }
    
    /**
     * Calcula los valores de DIS-C entre todos los conceptos utilizando el 
     * algoritmo de Dijkstra para el cálculo de las distancias
     * @return El valor de la variación del total de los nodos
     */
    public double calcularDijkstra() {
        double retVal = 0;
        Dijkstra dijk = new Dijkstra(Dijkstra.Element.EDGE, null, STR_PESO);
//        DISC = new double[disc.getNodeCount()][disc.getNodeCount()];
//        ArrayList<Node> nodes = new ArrayList<>(disc.getNodeSet());
        
        dijk.init(disc);
        
        // VERSION CON HASHMAP
        for( Node n : disc ) {
            dijk.setSource(n);
            dijk.compute();
            
            HashMap<String,Double> dist = new HashMap<>();
            n.setAttribute(STR_DISC_ATT, dist);
            for( Node m : disc ) {
                dist.put(m.getId(), dijk.getPathLength(m));
            }
        }
        
        for( Node n : disc) {
            double from = 0;
            double to   = 0;
            
            for( Node m : disc ) {
                from += ((HashMap<String,Double>)n.getAttribute(STR_DISC_ATT)).get(m.getId());
                to   += ((HashMap<String,Double>)m.getAttribute(STR_DISC_ATT)).get(n.getId());
            }
            
            double v_a = n.getAttribute(STR_GENERALIDAD);
            retVal += sqr( v_a - generalidad(from, to));
//            retVal += sqr( v_a - from / to);
            graph.getNode(n.getId()).setAttribute(STR_GENERALIDAD, generalidad(from, to));
        }
        
        return retVal / disc.getNodeCount();  
    }
    
    /**
     * Calcula la generalidad de un concepto dada su distancia desde y hacia todos 
     * los demás conceptos.
     * @param from distancia desde todos los demás conceptos.
     * @param to distancia hacia todos los demás comnceptos.
     * @return 
     */
    protected double generalidad(double from, double to) {
//        return from / (from + to);
        return from / to;
    }
    
    /**
     * Calcula los valores de DIS-C entre todos los conceptos utilizando el 
     * algoritmo de Landmarks para el cálculo de las distancias
     * @return El valor de la variación del total de los nodos
     */
    public double calcularFAPDE() {
        double retVal = 0;
        FAPDE fapde = new FAPDE();
        fapde.init(disc);
        fapde.setAtributoDePeso(STR_PESO);
        fapde.compute();
        
        for( Node nodeA : disc ) {
            double from = 0.0, to = 0.0;
            
            HashMap<String,Double> dist = new HashMap<>();
            nodeA.setAttribute(STR_DISC_ATT, dist);
            for( Node nodeB : disc ) {
                dist.put(nodeB.getId(), fapde.getDistance(nodeA, nodeB));
                from += fapde.getDistance(nodeA, nodeB);
                to   += fapde.getDistance(nodeB, nodeA); 
            }
            
            double v_a = (double)(nodeA.getAttribute(STR_GENERALIDAD));
            retVal += sqr(v_a - generalidad(from, to));
            graph.getNode(nodeA.getId()).setAttribute(STR_GENERALIDAD, generalidad(from, to));
        }

        return retVal / disc.getNodeCount();
    }
    
    /**
     * Calcula los valores de DIS-C entre todos los conceptos utilizando el 
     * algoritmo de Dijkstra implementado en forma matricial.
     * @return El valor de la variación del total de los nodos
     */
    public double calculaDijkstraMatrixCPU() {
        double retVal = 0;
        DijkstraAP_CL jocl = new DijkstraAP_CL();
        jocl.init(graph);
        jocl.setAtributoDePeso(STR_PESO);
        jocl.setGPU(false, 0);
        jocl.compute();
        
        for(int i=0; i<disc.getNodeCount(); i++) {
            double from = 0.0, to = 0.0;
            Node nodeA = jocl.getNode(i);
            
            HashMap<String,Double> dist = new HashMap<>();
            nodeA.setAttribute(STR_DISC_ATT, dist);
            for(int j=i; j<disc.getNodeCount(); j++) {             
                Node nodeB = jocl.getNode(j);

                dist.put(nodeB.getId(), (double)jocl.getDistance(i, j));
                from += jocl.getDistance(i, j);
                to   += jocl.getDistance(j, i); 
            }
            
            double v_a = (double)(nodeA.getAttribute(STR_GENERALIDAD));
            retVal += sqr(v_a - generalidad(from, to));
            graph.getNode(nodeA.getId()).setAttribute(STR_GENERALIDAD, generalidad(from, to));
        }
        
        return retVal / disc.getNodeCount();
    }
    
    /**
     * Calcula los valores de DIS-C entre todos los conceptos utilizando el 
     * algoritmo de Dijkstra implementado en forma matricial y calculado utilizando 
     * las GPU
     * @return El valor de la variación del total de los nodos
     */
    public double calculaDijkstraMatrixGPU() {
        double retVal = 0;
        DijkstraAP_CL jocl = new DijkstraAP_CL();
        jocl.init(graph);
        jocl.setAtributoDePeso(STR_PESO);
        jocl.compute();
        
        for(int i=0; i<disc.getNodeCount(); i++) {
            double from = 0.0, to = 0.0;
            Node nodeA = jocl.getNode(i);
            
            HashMap<String,Double> dist = new HashMap<>();
            nodeA.setAttribute(STR_DISC_ATT, dist);
            for(int j=i; j<disc.getNodeCount(); j++) {             
                Node nodeB = jocl.getNode(j);

                dist.put(nodeB.getId(), (double)jocl.getDistance(i, j));
                from += jocl.getDistance(i, j);
                to   += jocl.getDistance(j, i); 
            }
            
            double v_a = (double)(nodeA.getAttribute(STR_GENERALIDAD));
            retVal += sqr(v_a - generalidad(from, to));
            graph.getNode(nodeA.getId()).setAttribute(STR_GENERALIDAD, generalidad(from, to));
        }
        
        return retVal / disc.getNodeCount();
    }
    
    /**
     * Genera la traza del algoritmo en forma de una cadena
     * @return La cadena con la traza
     */
    @Override
    public String toString() {
        Formatter f = new Formatter(Locale.US);
        
        f.format("TRACE:\n%s", trace.toString() );
        f.format("GENERELITY:\n");
        ArrayList<Node> vis = new ArrayList<>(graph.getNodeSet());
        Collections.sort(vis, (Node o1, Node o2) -> {
            double g1 = o1.getAttribute(STR_GENERALIDAD);
            double g2 = o2.getAttribute(STR_GENERALIDAD);
            
            return g1>g2? -1 : (g1<g2? 1 : 0);
        });
        f.format("id, gen, qi\n");
        for( Node n:vis) {
            double gen = (double)n.getAttribute(STR_GENERALIDAD);
            String id = n.getId();
            f.format("%s, %10.8f, %10.8f\n", id, gen, -Math.log(gen));
        }
        
        return f.toString();
    }

//    /**
//     * Calcula el i-vecindario del nodo n
//     * @param n El nodo de referencia
//     * @param i El nivel del vecindario
//     * @return El i-vecindario del nodo n
//     */
//    public static HashSet<Node> neighborhood(Node n, int i) {
//        HashSet<Node> retVal = new HashSet<>();
//        
//        retVal.add(n);
//        
//        if(i > 0) {
//            Iterator<Node> ni = n.getNeighborNodeIterator();
//            while(ni.hasNext()) {
//                Node next = ni.next();
//                HashSet<Node> rec = neighborhood(next, i-1);
//                retVal.addAll(rec);
//                retVal.add(next);
//            }
//        }
//        
//        return retVal;
//    }
    
//    /**
//     * Calcula la afinidad de dos conceptos de acuerdo a la i-vecindad
//     * @param a Primer concepto
//     * @param b Segundo concepto
//     * @param vecindad El tamaño del vecindario
//     * @return El valor de la afinidad entre los conceptos a y b
//     */
//    public static double afinity(Node a, Node b, int vecindad) {
//        HashSet<Node> nA = neighborhood(a, vecindad);
//        HashSet<Node> nB = neighborhood(b, vecindad);
//        HashSet<Node> resta = (HashSet<Node>) nB.clone();
//        
//        resta.removeAll(nA);
//        return 1.0 - ((double)resta.size()/(double)nB.size());
//    }
//    
//    public double Pc(Node x, Node y, int vecindad) {
//        HashSet<Node> nX = neighborhood(x, vecindad);
//        double retVal = 0;
//        
//        for( Node v : nX ) {
//            double af = afinity(x, v, vecindad);
//            double dis = getDISC(v.getAttribute("name"), y.getAttribute("name"));
//            retVal += af*Math.pow(2, -(dis/2));
//        }
//        
//        return retVal / nX.size();
//    }
  
    /**
     * Genera la tabla de distancias conceptuales en código LaTEX
     * @return El código LaTEX de la tabla de distancias conceptuales
     */
    public String toLaTexTabular() {
        StringBuilder str = new StringBuilder("\\begin{tabular}{\n");
        for( int i=0; i<=disc.getNodeCount()+1; i++) str.append("|c"); 
        str.append("|} \\hline\n - &");
        for( Node n : disc.getEachNode() ) str.append(n).append(" & ");
        str.append("\\\\ \\hline\n");
        for(Node n:disc.getEachNode()) {
            str.append(n + " & ");
            for( Node m:disc.getEachNode()) {
//                str.append(String.format("%4.2f &", afinity(n, m, 1)));
                str.append(String.format("%6.4f ", getDISC(n.getId(), m.getId())));
            }
            str.append("\\\\ \\hline\n");
        }
        str.append("\\end{tabular}\n");
        
        return str.toString();
    }
    
    /**
     * Genera el codigo CSV de la tabla de distancias conceptuales
     * @return El código CSV de la tabla de distancias conceptuales
     */
    public String toCSV() {
        StringBuilder str = new StringBuilder("- ");
        ArrayList<Node> nodos = new ArrayList<>(disc.getNodeSet());
        
        nodos.sort((Node a, Node b) -> {
            return a.getId().compareTo(b.getId());
        });
        
        nodos.stream().forEach((Node n) -> { str.append(", ").append(n); });
        for( int i=0; i<nodos.size(); i++) {
            str.append("\n").append(nodos.get(i).getId());
            for( int j=0; j<nodos.size(); j++) {
                str.append(String.format(", %f", getDISC(nodos.get(i).getId(), nodos.get(j).getId())));
            }
        }
        str.append("\n");
        
        return str.toString();
    }

    /**
     * Da formato a un grafo de palabras
     * @param nodos Los nodos a partir de las que se generó el grafo
     */
    public void formatGraph(HashSet<String> nodos) {
        double max_degree = 0;
        
        /**
         * Calcular el grado máximo del grafo.
         */
        for( Node n:disc ) {
            if( n.getDegree() > max_degree ) 
                max_degree = n.getDegree();
        }
        
        /**
         * Dar formato a los nodos con el nombre del nodo como etiqueta, su tamaño depende
         * del grado, así como su color.
         */
        for( Node n:disc ) {
            int degree = n.getDegree();
            double pct = degree / max_degree;
            degree = (int) (50*pct + 10);
            int color = (int) (128 - 120*pct);
            
            n.setAttribute("ui.label", n.getId());
            n.setAttribute("ui.style", "text-size:"+ degree +";");
            n.setAttribute("ui.style", "text-color:rgb("+ color +","+ color +","+ color +");");
            n.setAttribute("ui.style", "size:6px; fill-color:white; ");
        }
        
        /**
         * Dar formato especia a las palabras que se están procesando.
         */
        if(nodos!=null) for( String w:nodos ) {
            Node n = disc.getNode(w);
            if( n==null ) continue;
            n.setAttribute("ui.style", "text-size:25;");
            n.setAttribute("ui.style", "text-color:rgb(120,50,50);");
            n.setAttribute("ui.style", "fill-color:rgb(250,250,250);");
            n.setAttribute("ui.style", "stroke-mode:plain;");
            n.setAttribute("ui.style", "stroke-color:rgb( 120, 50,50);");
            n.setAttribute("ui.style", "shape:rounded-box;");
            n.setAttribute("ui.style", "size-mode:fit;");
        }
        
        /**
         * Dar formato a las aristas dependiendo del grado mayor entre los nodos 
         * conecatdos.
         */
        for( Edge e:disc.getEachEdge() ) {
            int degree = max( e.getNode0().getDegree(), e.getNode1().getDegree() );
            double pct = degree / max_degree;
            int color = (int) (220 - 170*pct);
//            int color = 255 - 255/grado;
            
            e.setAttribute("ui.style", "fill-color:rgb("+ (color/1) +","+ (color/1) +","+ (color/1) +");");
            e.setAttribute("ui.style", "text-color:rgb("+ (color/1) +","+ (color/1) +","+ (color/1) +");");
        }
    }

}
