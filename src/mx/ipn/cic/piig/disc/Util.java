/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;

/**
 *
 * @author rolando
 */
public class Util {
    public static int verboseLevel = 0;
    public static final int PRINT_NOTHING = 0;
    public static final int PRINT_GENERAL = 1;
    public static final int PRINT_DETAIL = 2;
    public static final int PRINT_VERY_DETAIL = 3;
    
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
        } catch(IOException e) {
            System.err.println("No se pudo crear el archivo '"+ fileName +"'");
        }
    }
    
    /**
     * Obtiene el código GraphViz correspondiente al grafo g
     * @param g El grafo del que se quiere obtener el código de GraphViz
     * @return El código GraphViz del grafo g
     */
    public static String toViz(Graph g) {
        String retVal = "digraph abstract {\n" +
                    "graph [ rankdir = \"BT\" ];\n";
        
        for( Node n : g.getEachNode() ) {
            retVal += n.toString() + " [ ";
            Iterator<String> it = n.getAttributeKeyIterator();
            while(it.hasNext()) {
                String att = it.next();
                retVal += att +" = "+ n.getAttribute(att);
                if( it.hasNext() )
                    retVal += ",\n\t";
            }
            retVal += "];\n";
        }
        
        for( Edge e : g.getEachEdge() ) {
            retVal += e.getNode0() +" -> "+ e.getNode1() +" [ ";
            Iterator<String> it = e.getAttributeKeyIterator();
            while( it.hasNext() ) {
                String att = it.next();
                retVal += att + " = "+ e.getAttribute(att);
                if( it.hasNext() )
                    retVal += ",\n\t";
            }
            retVal += "];\n";
        }
        
        return retVal + "}\n";
    }
    
    public static void print(int nivel, String str) {
        if( nivel <= verboseLevel )
            System.out.print(str);
    }
    
    public static void println(int nivel, String str) {
        if( nivel <= verboseLevel )
            System.out.println(str);
    }
}
