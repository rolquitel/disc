/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import mx.ipn.cic.piig.disc.algoritmos.DISC;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 *
 * @author rolando
 */
public class Wikipedia2Graph {
    public static int MAX_DEPTH = 2;
    public static int LINES_PER_FILE = 100000;
    Graph graph;
    static final String workingDir = "/Users/rolando/Desktop/work/";
    static String linksTextFile = workingDir + "links.txt";
    static String linksTextFiles = workingDir + "wiki/file%03d.txt";
    static String linksSQLiteFile = workingDir + "wiki.sqlite";
    Connection conn;
    
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
    
    public static int max(int a, int b) {return a>b?a:b;}
    
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
    
    /**
     * Lee las parejas de palabras desde el archivo especificado.
     * @param fileName Nombre del archivo donde están las palabras a procesar.
     */
    public void readPairs(String fileName) {
        try {
            File file = new File(fileName);
            Scanner sc = new Scanner(file);
            while( sc.hasNextLine()) {
                String line = sc.nextLine();
                String palabras[] = line.split(",");
                addWordPair(palabras[0], palabras[1]);
            }
        } catch(FileNotFoundException e) {
            System.out.println(e);
        }
    }

    /**
     * Constructor de la clase
     * @throws SQLException 
     */
    public Wikipedia2Graph() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:"+linksSQLiteFile);
    }
    
    /**
     * Lee linea solicitada desde el archivo de links.
     * @param line Línea que se desea leer
     * @return El contenido de la línea en el archivo de links.txt
     * @throws Exception 
     */
    String getLine(int line) throws Exception {
        String filename = String.format("/Users/rolando/Desktop/work/wiki/file%03d.txt", line/LINES_PER_FILE);
        Scanner sc = new Scanner(new File(filename));
        String retVal = "";
        int i = 0;
        int lineOffset = line % LINES_PER_FILE;
        
        do {
            retVal = sc.nextLine();
            i++;
        } while(sc.hasNextLine() && i<lineOffset);
        
        return retVal;
    }
    
    /**
     * Obtiene los links de la pagina solicitada.
     * @param page Página solicitada
     * @return Arreglo con las páginas a las que se tiene link.
     */
    ArrayList<String> getLinks(String page) {        
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM pages WHERE lower(title)=lower(\""+ page +"\");");
            int id = -1;

            if( rs.next() ) 
                id = rs.getInt("id");
            else 
                return null;

            String line = getLine(id);
            String tokens[] = line.split("\\|");

            ArrayList<String> retVal = new ArrayList<>();
            for(int i=2; i<tokens.length; i++) {
                retVal.add(tokens[i]);
            }

            return retVal;
        } catch(Exception e) {
            return null;
        }
    }
    
    /**
     * Genera el grafo con las palabras añadidas
     * @return El grafo generado
     */
    public Graph toGraph() {
        this.graph = new SingleGraph("wiki2g");
        
        for(String word : words) {
            toGraph(null, word, 0, 0);
        }
                
        return this.graph;
    }
    
    /**
     * Función recursiva para construir un grafo a partir de una palabra
     * @param from Palabra de origen
     * @param to Palabra relacionada
     * @param depth Profundidad de la búsqueda
     * @param type Tipo de la relación entre from y to
     */
    public void toGraph(String from, String to, int depth, int type) {  
        boolean toExists = graph.getNode(to) != null;
        
        /**
         * Si existe un nodo de origen se debe agregar una nueva arista desde el nodo
         * de origen hacia el nodo destino; si el nodo de destino ya existe, entonces
         * se marca para que no se procese doble.
         */
        if(from != null) {
            try {
                if( graph.getNode(from) == null ) {
                    Node n = graph.addNode(from);
                }
                if( graph.getNode(to) == null ) {
                    Node n = graph.addNode(to);
                } else {
                    toExists = true;
                }
                Edge e = graph.addEdge(from+"->"+to, from, to, true); 
                e.addAttribute("type", type);
            } catch( IdAlreadyInUseException | ElementNotFoundException e) {}
        }
        
        /**
         * Si el nodo de destino no ha sido procesado, entonces se buscan sus conexiones
         * y se llama recursivamente a la función siempre y cuando no se haya exedido
         * la profundidad máxima.
         */
        for(int i=0; i<depth; i++) System.out.print("    ");
        if( depth<MAX_DEPTH && !toExists ) {
            System.out.print("Procesando '"+ to +"'. ");
            ArrayList<String> links = getLinks(to);
            if( links == null ) {
                System.out.println("No existe en la BD.");
            } else {
                System.out.println("Se agregarán "+ links.size() +" aristas.");
                if( links != null ) {
                    for(String link : links) {
                        toGraph(to, link, depth+1, 0);

                    }
                }
            }
        } else {
            System.out.println("Saltando '"+ to +"'");
        }
    }
    
    /**
     * Divide el archivo de ligas en varios archivos para que sea más rápido buscar
     * una página.
     * @throws Exception 
     */
    public static void splitInFiles() throws Exception {
        Scanner sc = new Scanner(new File(linksTextFile));
        int nLines = 0;
        int curFile = 0;
        
        BufferedWriter writer = null;
        
        while( sc.hasNextLine() ) {
            if( nLines % LINES_PER_FILE == 0) {
                String filename = String.format(linksTextFiles, curFile++);
                File f = new File(filename);
                f.createNewFile();
                writer = new BufferedWriter(new FileWriter(f));
                System.out.println("Nuevo archivo "+ f.getCanonicalPath());
            }
            writer.write(sc.nextLine() + "\n");
            nLines++;
        }
        writer.close();
    }
    
    /**
     * Poda el grafo desechando los nodos que tienen menos de minDegree aristas,
     * se ejecuta iterativamente hasta que ya no haya nodos que no cumplan las 
     * condiciones descritas, ya que cada vez que se quita un nodo se quitan las 
     * aristas conectadas a él, por lo que puede ocasionar que otros nodos no cumplan
     * las condiciones.
     * @param minDegree El grado mínimo que debe tener un grafo para no ser eliminado.
     */
    public void prune(int minDegree, int sleep, boolean format) throws Exception {
        boolean seguir = true;
        
        System.out.println("Podando (Nivel "+ minDegree +")...\nNodos: "+ this.graph.getNodeCount() +" Aristas: "+ this.graph.getEdgeCount());
        while(seguir) {
            seguir = false;
            for( Node n:this.graph ) {
                if( n.getDegree() < minDegree) {
                    if( !this.words.contains(n.getId().toLowerCase())) {
                        Iterator<Node> it = n.getNeighborNodeIterator();
                        boolean borrar = true;
                        while( it.hasNext() ) {
                            if( this.words.contains(it.next().getId().toLowerCase()) )
                                borrar = false;
                        }
                        if( borrar ) {
                            this.graph.removeNode(n);
                            seguir = true;
                            Thread.sleep(sleep);
                        }
                        
                    }
                }
            }
            if(format) formatGraph(graph, words);
            System.out.println("Nodos: "+ this.graph.getNodeCount() +" Aristas: "+ this.graph.getEdgeCount());
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
        
        /**
         * Calcular el grado máximo del grafo.
         */
        for( Node n:g ) {
            if( n.getDegree() > max_degree ) 
                max_degree = n.getDegree();
        }
        
        /**
         * Dar formato a los nodos con el nombre del nodo como etiqueta, su tamaño depende
         * del grado, así como su color.
         */
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
        
        /**
         * Dar formato especia a las palabras que se están procesando.
         */
        for( String w:words ) {
            Node n = g.getNode(w);
            if( n==null ) continue;
            n.setAttribute("ui.style", "fill-color:#88F;");
            n.setAttribute("ui.style", "stroke-mode:plain;");
            n.setAttribute("ui.style", "stroke-color:blue;");
            n.setAttribute("ui.style", "shape:rounded-box;");
            n.setAttribute("ui.style", "size-mode:fit;");
        }
        
        /**
         * Dar formato a las aristas dependiendo del grado mayor entre los nodos 
         * conecatdos.
         */
        for( Edge e:g.getEachEdge() ) {
            int grado = max( e.getNode0().getDegree(), e.getNode1().getDegree() );
            int color = 255 - 255/grado;
            
            e.setAttribute("ui.style", "fill-color:rgb("+ (color/2) +","+ (color/2) +","+ (color/1) +");");
            e.setAttribute("ui.style", "text-color:rgb("+ (color/2) +","+ (color/2) +","+ (color/1) +");");
        }
    }
    
    /**
     * Despliega el grafo y le da formato.
     */
    public void display() {
        graph.addAttribute("ui.antialias");
        graph.display();
        formatGraph(graph, words);
    }
    
    public static void main(String argfs[]) {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        String filename = "wiki30";
        boolean read = false;
        Graph g = new SingleGraph("saved");
        
        try {
            g.read(filename + ".dgs");
            read = true;
        } catch(Exception e) {
            
        }
        
        try {
            Wikipedia2Graph w = new Wikipedia2Graph();
            w.readPairs(filename + ".csv");  
            if( !read ) {
                w.toGraph();
                w.graph.write(filename + ".dgs");
            } else {
                w.graph = g;
            }
                   
//            w.display();
            w.prune(5, 0, false);
            
            DISC d = new DISC();
            d.init(g);
            d.compute();
//            disc.display();
            d.disc.write(filename + "_disc.dgs");
            
            formatGraph(d.disc, w.words);
            System.out.print("Guardanto traza... ");
            Util.toFile(filename + "_trace.txt", d.toString());
            System.out.print("OK.\nGuardando CSV... ");
            Util.toFile(filename + "_disc.csv", d.toCSV());
            System.out.println("Ok.");
            
            for( wordsPair p:w.wp ) {
                try {
                    System.out.printf(Locale.US, "%s, %s, %6.4f, %6.4f\n", p.a, p.b, d.getDISC(p.a, p.b), d.getDISC(p.b, p.a));
                } catch(Exception e) {
                    System.out.println("No existe camino entre la pareja ("+ p.a +","+ p.b +")");
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }        
    }
}
