/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import java.util.ArrayList;

/**
 *
 * @author rolando
 */
public class AllCorpus2Graph extends Corpus {
    WordNetCorpus wn;
    WikipediaCorpus wp;
    WikiCatCorpus wc;
    
    public AllCorpus2Graph() throws Exception {
        super();
    }

    @Override
    public int checkElem(String elem) {
        return max(wn.checkElem(elem), max(wp.checkElem(elem), wc.checkElem(elem)));
    }    

    @Override
    protected ArrayList<ArrayList<String>> getRelationships(String elem) {
        ArrayList<ArrayList<String>> retVal = new ArrayList<>();
        ArrayList<ArrayList<String>> tmp;
        
        for( int i=0; i<WordNetCorpus.NUM_RELATIONSHIPS; i++)
            retVal.add(new ArrayList<>());
        
        // Agrega relaciones de Wikipedia
        tmp = wp.getRelationships(elem);
        if( tmp != null ) for( String s : tmp.get(0) ) retVal.get(Corpus.WIKI_LINK).add(s);
        
        // Agrega relaciones de Wordnet
        tmp = wn.getRelationships(elem);
        for( int i=0; tmp!=null && i<tmp.size(); i++ ) {
            for( String s : tmp.get(i) ) retVal.get(i).add(s);
        }
        
        // Agrega relaciones de Categorías de Wikipedia
        tmp = wc.getRelationships(elem);
        if( tmp != null ) for( String s : tmp.get(0) ) retVal.get(Corpus.WIKI_LINK).add(s);
        
        return retVal;
    }

    @Override
    public void init() throws Exception {
        wn = new WordNetCorpus();
        wp = new WikipediaCorpus();
        wc = new WikiCatCorpus();
    }

    @Override
    public int getCard(String elem) {
        return wn.getCard(elem) + wp.getCard(elem) + wc.getCard(elem);
    }
    
}
