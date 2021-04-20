/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mx.ipn.cic.piig.disc;

import edu.smu.tspell.wordnet.*;
import java.util.ArrayList;
import org.graphstream.graph.*;

/**
 *
 * @author rolando
 */
public class WordNetCorpus extends Corpus {   
       
    protected WordNetDatabase database;
    protected boolean useType[];
    
    public WordNetCorpus() throws Exception {
        super();
    }
    
    /**
     * Establece si el tipo de relación será tomada en cuenta en la generación del 
     * grafo.
     * @param type La relación que puede ser HYPERNYMS, HYPONYMS, INSTANCE_HYPERNYMS,
     * INSTANCE_HYPONYMS, MEMBER_HOLONYMS, MEMBER_MERONYMS, PART_HOLONYMS o PART_MERONYMS
     * @param set falso o verdadero
     */
    public void setUseType( int type, boolean set ) {
        useType[type] = set;
    }
    
    @Override
    public void init() throws Exception {
        System.setProperty("wordnet.database.dir", "./WordNet-3.0/dict");
        database = WordNetDatabase.getFileInstance();
        useType = new boolean[]{ true, true, false, false, false, false, true, true };
    }

    @Override
    public int checkElem(String elem) {
        Synset sss[] = database.getSynsets(elem);
        
        if( sss == null ) return 0;
        
        return sss.length;
    }

    @Override
    protected ArrayList<ArrayList<String>> getRelationships(String elem) {
        ArrayList<ArrayList<String>> retVal = new ArrayList<>();
        Synset sss[] = database.getSynsets(elem);
        
        for( int i=0; i<NUM_RELATIONSHIPS; i++ ) retVal.add(i, new ArrayList<>());
        
        for( Synset ss : sss ) {
            if( ss.getType() == SynsetType.NOUN ) {
                NounSynset nss = (NounSynset)ss;
                if( useType[HYPONYMS] ) for( NounSynset hss:nss.getHyponyms() ) { retVal.get(HYPONYMS).add(hss.getWordForms()[0]); }
                if( useType[HYPERNYMS] ) for( NounSynset hss:nss.getHypernyms() ) { retVal.get(HYPERNYMS).add(hss.getWordForms()[0]); }
                if( useType[INSTANCE_HYPONYMS] ) for( NounSynset hss:nss.getInstanceHyponyms() ) { retVal.get(INSTANCE_HYPONYMS).add(hss.getWordForms()[0]); }
                if( useType[INSTANCE_HYPERNYMS] ) for( NounSynset hss:nss.getInstanceHypernyms() ) { retVal.get(INSTANCE_HYPERNYMS).add(hss.getWordForms()[0]); }
                if( useType[MEMBER_HOLONYMS] ) for( NounSynset hss:nss.getMemberHolonyms() ) { retVal.get(MEMBER_HOLONYMS).add(hss.getWordForms()[0]); }
                if( useType[MEMBER_MERONYMS] ) for( NounSynset hss:nss.getMemberMeronyms() ) { retVal.get(MEMBER_MERONYMS).add(hss.getWordForms()[0]); }
                if( useType[PART_HOLONYMS] ) for( NounSynset hss:nss.getPartHolonyms() ) { retVal.get(PART_HOLONYMS).add(hss.getWordForms()[0]); }
                if( useType[PART_MERONYMS] ) for( NounSynset hss:nss.getPartMeronyms() ) { retVal.get(PART_MERONYMS).add(hss.getWordForms()[0]); }
            }
        }
        
        return retVal;
    }

    @Override
    public int getCard(String elem) {
        ArrayList<ArrayList<String>> rels = getRelationships(elem);
        int retVal = 0;
        
        for( ArrayList<String> rel : rels ) {
            retVal += rel.size();
        }
        
        return retVal;
    }
}
