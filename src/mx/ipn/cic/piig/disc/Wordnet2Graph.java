/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import mx.ipn.cic.piig.disc.algoritmos.FAPDE;
import mx.ipn.cic.piig.disc.algoritmos.DISC;
import java.util.HashSet;
import java.util.Locale;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import edu.smu.tspell.wordnet.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.IdAlreadyInUseException;
/**
 * Clase para generar el grafo de pares de palabras basado en las relaciones
 * de WordNet
 * @author Rolando Quintero
 */
public class Wordnet2Graph {
    WordNetDatabase database;
    static int MAX_DEPTH = 2;
    String RELATIONS="HhIiMmPp"; //"HhIiMmPp"
    Graph theGraph;
    
    boolean SHOW_HYPERNYMS          = true;
    boolean SHOW_HYPONYMS           = true;
    boolean SHOW_INSTANCE_HYPERNYMS = false;
    boolean SHOW_INSTANCE_HYPONYMS  = false;
    boolean SHOW_MEMBER_HOLONYMS    = false;
    boolean SHOW_MEMBER_MERONYMS    = false;
    boolean SHOW_PART_HOLONYMS      = true;
    boolean SHOW_PART_MERONYMS      = true;

    /**
     * Genera el grafo con las palabras añadidas
     * @return El grafo generado
     */
    public Graph toGraph() {
        this.theGraph = new SingleGraph("wn2g");
        System.setProperty("wordnet.database.dir", "./WordNet-3.0/dict");
        database = WordNetDatabase.getFileInstance(); 
        
        for(String word : words) {
            toGraph(null, word, 0, this.theGraph, 0);
        }
        
        return this.theGraph;
    }
    
    /**
     * Función recursiva para construir un grafo a partir de una palabra
     * @param from Palabra de origen
     * @param to Palabra relacionada
     * @param depth Profundidad de la búsqueda
     * @param g Grafo que se construye
     * @param type Tipo de la relación entre from y to
     */
    public void toGraph(String from, String to, int depth, Graph g, int type) { 
        if(from != null) {
            try {
                if( g.getNode(from) == null ) {
                    Node n = g.addNode(from);
                }
                if( g.getNode(to) == null ) {
                    Node n = g.addNode(to);
                }
                Edge e = g.addEdge(from+"->"+to, from, to, true); 
                e.addAttribute("type", type);
            } catch( IdAlreadyInUseException | ElementNotFoundException e) {}
        }
        
        if( depth < MAX_DEPTH) {
            for(Synset ss : database.getSynsets(to)) {
                if( ss.getType() == SynsetType.NOUN ) {
                    NounSynset nss = (NounSynset)ss;
                    if(SHOW_HYPERNYMS) for(NounSynset hss:nss.getHypernyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 0);
                    }
                    if(SHOW_HYPONYMS) for(NounSynset hss:nss.getHyponyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 1);
                    }
                    if(SHOW_INSTANCE_HYPERNYMS) for(NounSynset hss:nss.getInstanceHypernyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 2);
                    }
                    if(SHOW_INSTANCE_HYPONYMS) for(NounSynset hss:nss.getInstanceHyponyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 3);
                    }
                    if(SHOW_MEMBER_HOLONYMS) for(NounSynset hss:nss.getMemberHolonyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 4);
                    }
                    if(SHOW_MEMBER_MERONYMS) for(NounSynset hss:nss.getMemberMeronyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 5);
                    }
                    if(SHOW_PART_HOLONYMS) for(NounSynset hss:nss.getPartHolonyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 6);
                    }
                    if(SHOW_PART_MERONYMS) for(NounSynset hss:nss.getPartMeronyms()) {
                        toGraph(to, hss.getWordForms()[0], depth+1, g, 7);
                    }
                }
            }
        }
    }
    
    /**
     * Da formato a un grafo de palabras
     * @param g El grafo a formatear
     * @param words Las palabras a partir de las que se generó el grafo
     */
    public static void formatGraph(Graph g, HashSet<String> words) {
        String []col = {"black", "gray", "yellow", "orange", "blue", "cyan", "red", "pink"};
        double max_degree = 0;
        
        for( Node n:g ) {
            if( n.getDegree() > max_degree ) 
                max_degree = n.getDegree();
        }
        
        for(Node n:g) {
            int degree = n.getDegree();
            double pct = degree / max_degree;
            degree = (int) (50*pct + 10);
            int color = (int) (128 - 120*pct);
            n.setAttribute("ui.label", n.getId());
            n.setAttribute("ui.style", "text-size:"+ degree +";");
            n.setAttribute("ui.style", "text-color:rgb("+ color +","+ color +","+ color +");");
            n.setAttribute("ui.style", "size:6px; fill-color:white; ");
        }
        
        for( String w:words ) {
            Node n = g.getNode(w);
            if( n==null ) continue;
            n.setAttribute("ui.label", n.getId());
            n.setAttribute("ui.style", "fill-color:#88F;");
            n.setAttribute("ui.style", "stroke-mode:plain;");
            n.setAttribute("ui.style", "stroke-color:blue;");
            n.setAttribute("ui.style", "shape:rounded-box;");
            n.setAttribute("ui.style", "size-mode:fit;");
        }
        
        for( Edge e:g.getEachEdge() ) {
            int type = e.getAttribute("type");
            if( type<0 ) {
                e.setAttribute("ui.style", "fill-color:white;");
                e.setAttribute("ui.style", "text-color:white;");
            } else {
                e.setAttribute("ui.style", "fill-color:"+ col[type] +";");
                e.setAttribute("ui.style", "text-color:"+ col[type] +";");
            }
            if( e.getAttribute("weight") != null ) {
                double w = e.getAttribute("weight");
                e.setAttribute("ui.label", String.format("%4.2f", w));
                
            }
        }
    }
    
    /**
     * Podar los terminos que quedan sueltos en el grafo
     */
    public void prune() throws Exception {
        boolean seguir = true;
        
        System.out.println("Nodos: "+ this.theGraph.getNodeCount() +" Aristas: "+ this.theGraph.getEdgeCount());
        while(seguir) {
            seguir = false;
            for( Node n:this.theGraph ) {
                if( n.getDegree() == 1) {
                    if( !this.words.contains(n.getId())) {
                        this.theGraph.removeNode(n);
                        seguir = true;
//                        Thread.sleep(10);
                    }
                }
            }
            System.out.println("Nodos: "+ this.theGraph.getNodeCount() +" Aristas: "+ this.theGraph.getEdgeCount());
        }
    }
    
    /**
     * Clase interna para manejar las parejas de palabras
     */
    class wordsPair {
        public String a, b;
        public wordsPair(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }
    
    HashSet<wordsPair> wp = new HashSet<>();
    HashSet<String> words = new HashSet<>();
    
    /**
     * Agraga una pareja de palabras
     * @param a Primera palabra
     * @param b Segunda palabra
     */
    public void addWordPair(String a, String b) {
        this.wp.add(new wordsPair(a, b));
        this.words.add(a);
        this.words.add(b);
    }
    
    /**
     * Agrega una sola palabra al conjunto de palabras
     * @param w La palabra a agragar
     */
    public void addWord(String w) {
        this.words.add(w);
    }
    
    public void readPairs(String fileName) {
        try {
            File file = new File(fileName);
            Scanner sc = new Scanner(file);
            while( sc.hasNextLine()) {
                String line = sc.nextLine();
                String words[] = line.split(",");
                addWordPair(words[0], words[1]);
            }
        } catch(Exception e) {
            System.out.println(e);
        }
    }
    
    /**
     * Genera el codigo CSV de la tabla de distancias conceptuales
     * @return El código CSV de la tabla de distancias conceptuales
     */
    public static String toCSV(Graph g, FAPDE fapde) {
        StringBuilder str = new StringBuilder("-, ");
        
        ArrayList<Node> nodos = new ArrayList<>();
        nodos.addAll(g.getNodeSet());
        nodos.sort((Node a, Node b) -> {
            return a.getId().compareTo(b.getId());
        });
        
        nodos.stream().forEach((Node n) -> { str.append(n).append(", "); });
        str.append("\n");

        for(Node n:nodos) {
            str.append(n).append(", ");
            for( Node m:nodos) {
//                str.append(String.format("%4.2f &", afinity(n, m, 1)));
                str.append(String.format("%6.4f, ", fapde.getDistance(n, m)));
            }
            str.append("\n");
        }
        
        return str.toString();
    }
    
    public static String toArchivoGrafoSimple(Graph g) {
        String retVal = "";
        
        for( Edge e : g.getEachEdge() ) {
            retVal += e.getNode0().getId().replace(" ", "_") +" "+ e.getNode1().getId().replace(" ", "_") +" "+ e.getAttribute("type") +"\n";
        }
        
        return retVal + "--END--";
    }
    
    /**
     * Genera un archivo fileName que contiene la cadena result
     * @param fileName Nombre del archivo
     * @param result Contenido del archivo
     */
    public static void toFile(String fileName, String result) {
        try {
            FileOutputStream file = new FileOutputStream(fileName);
            file.write(result.getBytes());
            file.close();
        } catch(Exception e) {
            System.err.println("No se pudo crear el archivo '"+ fileName +"'");
        }
    }
    
    public static void main(String[] args) {
        String dir = "old_results/";
        Wordnet2Graph w = new Wordnet2Graph();
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        DISC disc = new DISC();
        try {
            MAX_DEPTH = 2;
            String filename = "mc25";
            w.readPairs(dir + filename + ".csv");    
            Graph g = new SingleGraph(filename);
            try {
                System.out.print("Leyendo grafo ("+ filename +") ... ");
                g.read(dir + filename + ".dgs");
            } catch ( IOException ioe) {
                System.out.print("No.\nCreando grafo ... ");
                g = w.toGraph();
                System.out.print("Ok.\nPodando ... ");
                w.prune();
                System.out.print("Ok.\nGuardando ... ");
                g.write(dir + filename + ".dgs");
            }
            System.out.println("Ok");
            
            g.addAttribute("ui.antialias");
            
            for( Edge e : g.getEachEdge() ) { 
                e.setAttribute("weight", 1.0);
            }
            
//            DijstraTree dt = new DijstraTree();
//            dt.init(g);
//            dt.setSeed("love");
//            dt.compute();
//            Graph dij = dt.getTree();
//            //formatGraph(dij, w.words);
//            dij.display();
            
//            FAPDE fapde = new FAPDE();
//            fapde.init(g);
//            fapde.compute();
//            System.out.print("Escribiendo archivo ...");
//            toFile("fapde.csv", toCSV(g, fapde));
//            System.out.println("OK.");
            
            System.out.print("Calculando DISC ... ");
            disc.init(g);
            disc.setAlgoDeCalculoDeDistancias(DISC.AlgoDeCalculoDeDistancias.USE_DIJKSTRA);
            disc.compute();
            System.out.println("Ok.");
//            disc.display();
//            disc.disc.addAttribute("ui.antialias");
//            formatGraph(disc.disc, w.words);
//            System.out.println(disc.toString());
//            System.out.print("Guardando CSV... ");
//            Util.toFile(dir + filename + "_disc.csv", disc.toCSV());
//            System.out.println("Ok.");
            
        } catch(Exception e) {
            System.out.println(e);
            System.exit(0);
        }    
        
        for( wordsPair p:w.wp ) {
            try {
                System.out.printf(Locale.US, "%s, %s, %6.4f, %6.4f\n", p.a, p.b, disc.getDISC(p.a, p.b), disc.getDISC(p.b, p.a));
            } catch(Exception e) {
                System.out.println(e);
            }
        }
    }
}
