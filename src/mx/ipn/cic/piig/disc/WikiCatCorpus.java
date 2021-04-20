/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 *
 * @author rolando
 */
public class WikiCatCorpus extends Corpus {
    Connection conn;
    HashSet<String> hCats;

    @Override
    public void init() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection("jdbc:mysql://localhost/enwiki","rolando","r0l1t4s");
        
        hCats = new HashSet<>();
        hCats.add("Commons_category_link_is_on_Wikidata");
        hCats.add("Categories_requiring_diffusion");
        hCats.add("Container_categories");
        hCats.add("Wikipedia_extended-confirmed-protected_pages");
    }
    
    @Override
    public int checkElem(String elem) {
        elem = elem.replace(' ', '_');
        try {
            String sqlOld = "SELECT COUNT(p.page_title) AS cat "
                    + "FROM page AS p, categorylinks AS cl "
                    + "WHERE p.page_id=cl.cl_from "
                    + "AND cl.cl_type = 'subcat' "
                    + "AND cl.cl_to like '"+ elem +"%';";
            String sql = "SELECT COUNT(cl.cl_to) AS cat "
                    + "FROM categorylinks AS cl, page "
                    + "WHERE cl.cl_type='subcat' "
                    + "AND cl.cl_from=page.page_id "
                    + "AND page.page_title like '"+ elem +"';";
            Util.print(Util.PRINT_DETAIL, sql + " - ");
            ResultSet rs = conn.createStatement().executeQuery(sql);

            while( rs.next() ) {
                return rs.getInt("cat");
            }
            
            return 0;
        } catch(Exception e) {
            return 0;
        }
    }
    
    @Override
    protected ArrayList<ArrayList<String>> getRelationships(String elem) {
        elem = elem.replace(' ', '_');
        try {
            // Esta consulta busca las subcategorias de 'elem'
            String sqlOld = "SELECT p.page_title AS cat "
                    + "FROM page AS p, categorylinks AS cl "
                    + "WHERE p.page_id=cl.cl_from "
                    + "AND cl.cl_type = 'subcat' "
                    + "AND cl.cl_to like '"+ elem +"%';";
            // Esta consulta busca las supercategorias de 'elem'
            String sql = "SELECT cl.cl_to AS cat "
                    + "FROM categorylinks AS cl, page "
                    + "WHERE cl.cl_type='subcat' "
                    + "AND cl.cl_from=page.page_id "
                    + "AND page.page_title like '"+ elem +"';";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            ArrayList<ArrayList<String>> retVal = new ArrayList<>();
            ArrayList<String> rel0 = new ArrayList<>();
            
            while( rs.next() ) {
                String cat = rs.getString("cat");
                if( !hCats.contains(cat) )
                    rel0.add(cat);
            }
            
            retVal.add(rel0);

            return retVal;
        } catch(Exception e) {
            return null;
        }
    }

    /**
     * Constructor de la clase
     * @throws SQLException 
     */
    public WikiCatCorpus() throws Exception {
        super();
    }

    @Override
    public int getCard(String elem) {
        return checkElem(elem);
    }

}
