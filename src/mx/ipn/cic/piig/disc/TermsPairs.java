/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author rolando
 */
public class TermsPairs {
    ArrayList<TermsPair> pairs = new ArrayList<>();
    ArrayList<String> terms = new ArrayList<>();
    
    public class TermsPair {
        public String a, b;
    
        public TermsPair(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }
    
    /**
     * Agraga una pareja de palabras
     * @param a Primera palabra
     * @param b Segunda palabra
     */
    public void addPair(String a, String b) {
        this.pairs.add(new TermsPair(a, b));
        this.terms.add(a);
        this.terms.add(b);
    }
    
    /**
     * Agrega una sola palabra al conjunto de palabras
     * @param w La palabra a agragar
     */
    public void addWord(String w) {
        this.terms.add(w);
    }
    
    /**
     * Lee las parejas de palabras desde el archivo especificado.
     * @param fileName Nombre del archivo donde están las palabras a procesar.
     */
    public void readPairs(String fileName) {
        try {
            File file = new File(fileName + ".csv");
            Scanner sc = new Scanner(file);
            while( sc.hasNextLine()) {
                String line = sc.nextLine();
                String palabras[] = line.split(",");
                addPair(palabras[0].trim(), palabras[1].trim());
            }
        } catch(FileNotFoundException e) {
            System.out.println(e);
        }
    }
}
