/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.graphstream.graph.*;

/**
 *
 * @author rolando
 */
public class WikipediaCorpus extends Corpus {
    public static int LINES_PER_FILE = 100000;
    static final String workingDir = "/Users/rolando/work/";
    static String linksTextFile = workingDir + "links.txt";
    static String linksTextFiles = workingDir + "wiki/file%03d.txt";
    static String linksSQLiteFile = workingDir + "wiki.sqlite";
    Connection conn;
    
    public WikipediaCorpus() throws Exception {
        super();
    }
    
    /**
     * Lee linea solicitada desde el archivo de links.
     * @param line Línea que se desea leer
     * @return El contenido de la línea en el archivo de links.txt
     * @throws Exception 
     */
    public static String getLine(int line) throws Exception {
        String filename = String.format("/Users/rolando/work/wiki/file%03d.txt", line/LINES_PER_FILE);
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

    @Override
    public void init() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite:"+linksSQLiteFile);
    }

    @Override
    public int checkElem(String elem) {
        try {
//            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM pages WHERE lower(title)=lower(\""+ elem +"\");");
            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM pages WHERE title like '"+ elem +"';");

            if( rs.next() ) 
                return rs.getInt("id");
            else 
                return 0;

        } catch(Exception e) {
            return 0;
        }
    }

    @Override
    protected ArrayList<ArrayList<String>> getRelationships(String elem) {
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM pages WHERE title like '"+ elem +"';");
//            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM pages WHERE lower(title)=lower(\""+ elem +"\");");
            int id = -1;

            if( rs.next() ) 
                id = rs.getInt("id");
            else 
                return null;

            String line = getLine(id);
            String tokens[] = line.split("\\|");

            ArrayList<ArrayList<String>> retVal = new ArrayList<>();
            ArrayList<String> rel0 = new ArrayList<>();
            for(int i=2; i<tokens.length; i++) {
                rel0.add(tokens[i]);
            }

            retVal.add(rel0);
            return retVal;
        } catch(Exception e) {
            return null;
        }
    } 

    @Override
    public int getCard(String elem) {
        try {
//            ResultSet rs = conn.createStatement().executeQuery("SELECT id FROM pages WHERE lower(title)=lower(\""+ elem +"\");");
            ResultSet rs = conn.createStatement().executeQuery("SELECT count(id) as card FROM pages WHERE title like '"+ elem +"';");

            if( rs.next() ) 
                return rs.getInt("card");
            else 
                return 0;

        } catch(Exception e) {
            return 0;
        }
    }
    
    public static void main( String arg[] ) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/wikipediaCorpus","rolando","r0l1t4s");
            
            Scanner sc = new Scanner(new File("/Users/rolando/Desktop/work/links.txt"));
            int id = 1;
            String line;

            while( sc.hasNextLine() ) {
                line = sc.nextLine();
                String tokens[] = line.split("\\|");
                String sql;
                String sqlUpdateDegree = "UPDATE pages "
                        + "SET outdegree="+ (tokens.length - 2) + " "
                        + "WHERE id="+ id +";";
                conn.createStatement().executeUpdate(sqlUpdateDegree);
                
                for( int i=2; i<tokens.length; i++ ) {
                    System.out.print("Insertar ( "+ tokens[0] +" , "+ tokens[i] +" ) ... ");
                    sql = "SELECT id FROM pages WHERE title like \""+ tokens[i] +"\"";
                    ResultSet tokenIds = conn.createStatement().executeQuery(sql);
                    if( !tokenIds.next() ) continue;
                    int tokId = tokenIds.getInt("id");
                    
                    sql = "INSERT INTO links VALUES ("+ id +","+ tokId +")";                    
                    sqlUpdateDegree = "UPDATE pages "
                            + "SET indegree=indegree+1 "
                            + "WHERE id="+ tokId +";";

                    try { 
                        conn.createStatement().executeUpdate(sql); 
                        conn.createStatement().executeUpdate(sqlUpdateDegree);
                        System.out.println("Ok.");
                    }
                    catch(Exception e) { System.out.println(e); }     
                }   
                
                id++;
            }             
        } catch(Exception e) {
            System.out.println(e);
        }
        
    }
}
