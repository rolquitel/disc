/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import java.io.File;
import java.util.*;
import mx.ipn.cic.piig.disc.algoritmos.*;
import org.graphstream.graph.*;
import org.graphstream.ui.view.Viewer;

/**
 *
 * @author rolando
 */
public class Main {
    public static String resultsDir = "results/";
    public static String setsDir = "sets/";
    public static double pExp = 1.0/3.0;
 
    public static Graph4Terms getGraph(String set, Corpus corpus, ArrayList<String> terms) throws Exception {
        Graph4Terms retVal = new Graph4Terms();
        String corpusName = corpus.getClass().getSimpleName();
        
        retVal.terms = terms;
        Util.print(Util.PRINT_GENERAL, "Tratando de leer el grafo ... ");
        
        if( !retVal.readGraph(resultsDir + set ) ) {
            int np = 0;
            Util.println(Util.PRINT_GENERAL, "No.\nChecando terminos ... ");
            for( String w : terms ) {
                if( corpus.checkElem(w) > 0 ) {
                    np++;
//                    Util.println(Util.PRINT_DETAIL, w +" SI");
                }
                else Util.println(Util.PRINT_DETAIL, w +" NO");
            }
            Util.println(Util.PRINT_GENERAL,String.format("%d de %d terminos están en el corpus\n", np, terms.size()));
            Util.print(Util.PRINT_GENERAL, "Creando grafo ... ");
            retVal = corpus.toGraph(set, terms);
            retVal.graph.write(resultsDir + set + ".dgs");
        }
        Util.println(Util.PRINT_GENERAL, retVal + " Ok.");
        
        Util.print(Util.PRINT_GENERAL, "Tratando de leer el grafo ... ");
        Graph4Terms temp = new Graph4Terms();
        if( !temp.readGraph(resultsDir + set + "_proc_"+ pExp) ) {
            retVal.expand(corpus, pExp); 
            retVal.prune(2, corpus); 
            retVal.graph.write(resultsDir + set + "_proc_"+ pExp +".dgs");
        } else {
            retVal.graph = temp.graph;
        }
        Util.println(Util.PRINT_GENERAL, retVal + " Ok.");
        
        return retVal;
    }
    
    public static Graph4Terms getGraph(String set, Corpus corpus, String t1, String t2, String midTerm) throws Exception {
        Graph4Terms retVal = new Graph4Terms();
        String corpusName = corpus.getClass().getSimpleName();
        ArrayList<String> terms = new ArrayList<>();
        String graphName = resultsDir + set + "_" + t1 + "--" + t2;
        
        terms.add(t1);
        terms.add(t2);
        
        retVal .terms = terms;
        
        Util.print(Util.PRINT_GENERAL, "Tratando de leer el grafo procesado ... ");
        if( !retVal.readGraph(graphName + "_processed") ) {
            Util.print(Util.PRINT_GENERAL, "No.\nTratando de leer el grafo expandido ... ");
            if( !retVal.readGraph(graphName + "_expanded" + pExp) ) {
                Util.print(Util.PRINT_GENERAL, "No.\nTratando de leer el grafo ... ");
                if( !retVal.readGraph(graphName) ) {
                    Util.print(Util.PRINT_GENERAL, "No.\nCreando grafo ... ");
                    retVal = corpus.toGraphDFS(t1, t2, midTerm,  Corpus.EARLY_RETURN, Corpus.SHOW_GRAPH);
                    if( retVal == null ) return null;
                    retVal.graph.write(graphName + ".dgs");
                }
                
                Util.print(Util.PRINT_GENERAL, "Ok\nExpandir ... ");
                retVal.expand(corpus, pExp);
                retVal.graph.write(graphName + "_expanded"+ pExp +".dgs");
            }

            Util.print(Util.PRINT_GENERAL, "Ok\nContraer ... ");
            retVal.prune(2, corpus);
            retVal.graph.write(graphName + "_processed.dgs");
        } 

        Util.print(Util.PRINT_GENERAL, "Ok\n");
        return retVal;
    }
    
    public static void procEachPair(String set, Corpus corpus, String midTerm) throws Exception {
        String corpusName = corpus.getClass().getSimpleName();
        TermsPairs tp = new TermsPairs();
        Graph4Terms g4;

        resultsDir += set +"/"+ corpusName +"/eachPair/";
        new File(resultsDir).mkdirs();
        
        tp.readPairs(setsDir + set);
        
        ArrayList<Graph4Terms> graphs = new ArrayList<>();
        
        System.out.println("Calculando distancia por cada par ...");
//        for( TermsPairs.TermsPair p : tp.pairs ) {
//            Util.print(Util.PRINT_GENERAL, p.a + " - " + corpus.checkElem(p.a) + "\n");
//            Util.print(Util.PRINT_GENERAL, p.b + " - " + corpus.checkElem(p.b) + "\n");
//        }
        
        for( TermsPairs.TermsPair p : tp.pairs ) {
            Util.print(Util.PRINT_GENERAL, String.format(Locale.US, "\nProcesando (%s, %s)\n", p.a, p.b));

            g4 = getGraph(set, corpus, p.a, p.b, midTerm);
            if( g4 == null ) {
                Util.print(Util.PRINT_GENERAL, "No se conectaron");
                continue;
            }
            graphs.add(g4);
            DISC disc = new DISC();
            disc.init(g4.graph);
            disc.setAlgoDeCalculoDeDistancias(DISC.AlgoDeCalculoDeDistancias.USE_DIJKSTRA);
            disc.compute();
            System.out.printf(Locale.US, "%s, %s, %6.4f, %6.4f\n", p.a, p.b, disc.getDISC(p.a, p.b), disc.getDISC(p.b, p.a));
        }
        Graph4Terms base = graphs.get(0);
        for( int i=1; i<graphs.size(); i++) {
            base.merge(graphs.get(i));
        }
        base.display();
        
        System.out.println("\nProcesando distancia en el grafo unificado ...");
        DISC disc = new DISC();
        disc.init(base.graph);
        disc.setAlgoDeCalculoDeDistancias(DISC.AlgoDeCalculoDeDistancias.USE_DIJKSTRA);
        disc.compute();
        for( TermsPairs.TermsPair p : tp.pairs ) {
            System.out.printf(Locale.US, "%s, %s, %6.4f, %6.4f\n", p.a, p.b, disc.getDISC(p.a, p.b), disc.getDISC(p.b, p.a));
        }
    }
    
    public static void procClassic(String set, Corpus corpus) throws Exception {
        String corpusName = corpus.getClass().getSimpleName();
        TermsPairs tp = new TermsPairs();
        Graph4Terms g4;
        
        Util.verboseLevel = Util.PRINT_DETAIL;
        
        resultsDir += set +"/"+ corpusName +"/";
        new File(resultsDir).mkdirs();
        
        tp.readPairs(setsDir + set);
        g4 = getGraph(set, corpus, tp.terms);
        g4.display();

        ConnectedSubgraphs cs = new ConnectedSubgraphs();
        cs.init(g4.graph);
        cs.compute();
        ArrayList<Graph> gs = cs.getSubgraphs();
        Util.println(Util.PRINT_DETAIL, String.format("Hay %d componente(s) conectado(s)\n", cs.getSubgraphCount()));

//        Util.print(Util.PRINT_GENERAL,"Ejecutando DISC... ");
        DISC disc = new DISC();
        disc.init(g4.graph);
        disc.setAlgoDeCalculoDeDistancias(DISC.AlgoDeCalculoDeDistancias.USE_DIJKSTRA);
        disc.compute();
//        disc.disc.write(resultsDir + "disc.dgs");
//        disc.display();
//        Util.println(Util.PRINT_GENERAL,"Ok.");
        
//        print("Guardando traza... ");
//        Util.toFile(resultsDir + "traza.txt", disc.toString());
//        println("Ok.");
        
//        print("Guardando CSV... ");
//        Util.toFile(resultsDir + "disc.csv", disc.toCSV());
//        println("Ok.");
        
        Util.print(Util.PRINT_GENERAL,"Guardando pares... ");
        String pares = "";
        for( TermsPairs.TermsPair p : tp.pairs ) {
            try {
                pares += String.format(Locale.US, "%s, %s, %6.4f, %6.4f\n", p.a, p.b, disc.getDISC(p.a, p.b), disc.getDISC(p.b, p.a));
            } catch(Exception e) {
//                pares += String.format("%s, %s, Infinity, Infinity\n", p.a , p.b );
            }
        }
        Util.toFile(resultsDir + "pares.csv", pares);
        Util.println(Util.PRINT_GENERAL,"Ok.");
        
        Util.print(Util.PRINT_GENERAL,"Guardando QI... ");
        String qi = "term, g(term), q_i(term)\n";
        for( String term : g4.terms ) {
            Node n = g4.graph.getNode(term);
            if( n == null ) continue;
            double gen = (double) n.getAttribute(DISC.STR_GENERALIDAD);
            qi += String.format("%s, %8.6f, %8.6f\n", term, gen, -Math.log(gen));
        }
        Util.toFile(resultsDir + "QI.csv", qi);
        Util.println(Util.PRINT_GENERAL,"Ok.");
    }
    
        
    public static void main(String args[]) throws Exception {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        Util.verboseLevel = Util.PRINT_DETAIL;
        //procEachPair("wiki30cat", new WikiCatCorpus(), "Main_topic_classifications");
        procEachPair("mc5", new WordNetCorpus(), "entity");
    }
}
    
