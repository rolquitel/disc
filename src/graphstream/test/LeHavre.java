/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphstream.test;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;

/**
 *
 * @author rolando
 */
public class LeHavre {
    public static void main(String args[]) {
        new LeHavre();
    }

    public LeHavre() {
        Graph graph = new MultiGraph("Le Havre");

        try {
                graph.read("LeHavre.dgs");
        } catch(Exception e) {
                e.printStackTrace();
                System.exit(1);
        }
        
        graph.addAttribute("ui.stylesheet", "url(file:///Users/rolando/Google Drive/programacion/java/GraphStream/lehavre.css)");
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");
        
        for(Edge edge: graph.getEachEdge()) {
            if(edge.hasAttribute("isTollway")) {
                    edge.addAttribute("ui.class", "tollway");
            } else if(edge.hasAttribute("isTunnel")) {
                    edge.addAttribute("ui.class", "tunnel");
            } else if(edge.hasAttribute("isBridge")) {
                    edge.addAttribute("ui.class", "bridge");
            }
            
            double speedMax = edge.getNumber("speedMax") / 130.0;
            edge.setAttribute("ui.color", speedMax);
        }
        
        graph.display(false);        
        
    }
}
