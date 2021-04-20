/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import mx.ipn.cic.piig.disc.algoritmos.DijkstraAP_CL;
import mx.ipn.cic.piig.disc.algoritmos.FAPDE;
import java.io.File;
import java.util.ArrayList;
import org.graphstream.algorithm.APSP;
import org.graphstream.algorithm.APSP.APSPInfo;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

/**
 *
 * @author rolando
 */
public class TestDistanceComputing {
    Graph graph;
    ArrayList<Node> nodes;
    
    protected static Graph generate(int nNodes) {
        Graph g = new SingleGraph("graph_"+ nNodes);
        Generator gen = new RandomEuclideanGenerator();
        
        gen.addSink(g);
        gen.begin();
        for( int i=0; i<nNodes; i++ ) {
            gen.nextEvents();
        }
        gen.end();
        
        for( Edge e:g.getEachEdge() ) {
            e.addAttribute("weight", 1 + Math.random()*9 );
        }
        
        return g;
    }
    
    public void init(String file) throws Exception {
        File f = new File(file + "_disc.dgs");
        
        if( f.exists() ) {
            System.out.println("Cargando grafo ...");
            graph = new SingleGraph(file);
            graph.read(file + "_disc.dgs");
        } else {
            System.out.println("Generando grafo ...");
            graph = generate(500);
        }
        
//        graph.display();

        nodes = new ArrayList<>(graph.getNodeSet());
        nodes.sort((Node a, Node b) -> {
            return a.getId().compareTo(b.getId());
        });
    }
    
    public void printArray(double dist[][]) {
        for(int i=0; i<nodes.size(); i++ ) {
            System.out.print(" ,"+ nodes.get(i).getId());
        }
        
        for(int i=0; i<nodes.size(); i++ ) {
            System.out.print("\n"+ nodes.get(i).getId());
            for(int j=0; j<nodes.size(); j++ ) {
                System.out.print(","+ dist[i][j]);
            }
        }
    }
    
    public double[][] calcularFloyd() {
        System.out.print("Calculando Floyd ... ");
        long tIni = System.currentTimeMillis();
        
        APSP apsp = new APSP(graph);
        apsp.setDirected(false);
        apsp.setWeightAttributeName("weight");
        apsp.compute();
        
        long tFin = System.currentTimeMillis();
        System.out.printf("%d ms.\n", tFin-tIni);

        double dist[][] = new double[graph.getNodeCount()][graph.getNodeCount()];

        for(int i=0; i<nodes.size(); i++) {
            for(int j=i; j<nodes.size(); j++) {
                if( i==j ) {
                    dist[i][j] = 0;
                    continue;
                }
                Node nodeA = nodes.get(i);
                Node nodeB = nodes.get(j);

                APSPInfo infA = nodeA.getAttribute(APSPInfo.ATTRIBUTE_NAME);
                APSPInfo infB = nodeB.getAttribute(APSPInfo.ATTRIBUTE_NAME);

                dist[i][j] = infA.getLengthTo(nodeB.getId());
                dist[j][i] = infB.getLengthTo(nodeA.getId());
            }
        }

        return dist;    
    }
    
    public double[][] calcularDijkstra() {
        System.out.print("Calculando Dijkstra ...");
        long tIni = System.currentTimeMillis();
        
        Dijkstra dijk = new Dijkstra(Dijkstra.Element.EDGE, null, "weight");
        double dist[][] = new double[graph.getNodeCount()][graph.getNodeCount()];
        
        dijk.init(graph);

        for(int i=0; i<nodes.size(); i++) {
            dijk.setSource(nodes.get(i));
            dijk.compute();
           
            for(int j=0; j<nodes.size(); j++) {
                if( i==j ) {
                    dist[i][j] = 0;
                    continue;
                }
                Node target = nodes.get(j);

                dist[i][j] = dijk.getPathLength(target);
            }
        }
        long tFin = System.currentTimeMillis();
        System.out.printf("%d ms.\n", tFin-tIni);
        
        return dist;    
    }
    
    public double[][] calcularFAPDE() {
        System.out.print("Calculando FAPDE ...");
        long tIni = System.currentTimeMillis();
        
        FAPDE fapde = new FAPDE();
        fapde.init(graph);
        fapde.compute();
        
        long tFin = System.currentTimeMillis();
        System.out.printf("%d ms.\n", tFin-tIni);

        double dist[][] = new double[graph.getNodeCount()][graph.getNodeCount()];

        for(int i=0; i<nodes.size(); i++) {
            for(int j=i; j<nodes.size(); j++) {
                if( i==j ) {
                    dist[i][j] = 0;
                    continue;
                }
                Node nodeA = nodes.get(i);
                Node nodeB = nodes.get(j);

                dist[i][j] = fapde.getDistance(nodeA, nodeB);
                dist[j][i] = fapde.getDistance(nodeB, nodeA); 
            }
        }
        
        
        return dist;    
    }
    
    public double[][] calcularDijkstra_Matrix() {
        System.out.print("Calculando Dijkstra_Matrix ...");
        long tIni = System.currentTimeMillis();
        
        DijkstraAP_CL jocl = new DijkstraAP_CL();
        jocl.init(graph);
//        jocl.setGPU(false, 0);
        jocl.compute();
        
        double dist[][] = new double[graph.getNodeCount()][graph.getNodeCount()];
        
        for(int i=0; i<nodes.size(); i++) {
            for(int j=i; j<nodes.size(); j++) {
                if( i==j ) {
                    dist[i][j] = 0;
                    continue;
                }

                dist[i][j] = jocl.getDistance(i, j);
                dist[j][i] = jocl.getDistance(j, i); 
            }
        }
        
        long tFin = System.currentTimeMillis();
        System.out.printf("%d ms.\n", tFin-tIni);
        
        return dist;
    }
    
    public static double ECM( double a[][], double b[][]) {
        int n = a.length;
        double retVal = 0;
        
        for( int i=0; i<n; i++ ) {
            for( int j=0; j<n; j++ ) {
                retVal += (a[i][j] - b[i][j]) * (a[i][j] - b[i][j]);
            }
        }
        
        return retVal / (n*n);
    }
    
    public static void main(String args[]) {
        String filename = "mc20";
        
        try {
            TestDistanceComputing test = new TestDistanceComputing();
            test.init(filename);
            
            System.out.printf("Grafo '%s' con %d nodos.\n", filename, test.graph.getNodeCount());
            
//            double floyd[][] = test.calcularFloyd();
            double dijkstra[][] = test.calcularDijkstra();
//            double fapde[][] = test.calcularFAPDE();
            double jocl[][] = test.calcularDijkstra_Matrix();
            
            
//            System.out.println("ECM(floyd, dijkstra) = "+ ECM(floyd, dijkstra));
//            System.out.println("ECM(floyd, fapde) = "+ ECM(floyd, fapde));
//            System.out.println("ECM(dijkstra, fapde) = "+ ECM(dijkstra, fapde));
//            System.out.println("ECM(floyd, jocl) = "+ ECM(floyd, jocl));
            System.out.println("ECM(dijkstra, jocl) = "+ ECM(dijkstra, jocl));
            
//            test.printArray(test.calcularFloyd());
//            test.printArray(dijkstra);
//            test.printArray(jocl);
//            test.printArray(test.calcularFAPDE());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
