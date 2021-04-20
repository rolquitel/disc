/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphstream.test;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
 
public class Tutorial1 {
        public static void main(String args[]) {
            System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
                Graph graph = new SingleGraph("Tutorial 1");
 
                Node n = graph.addNode("A"); 
                n.addAttribute("ui.label", "Nodo A");
                n.addAttribute("ui.style", "text-alignment:center; shape:circle; size-mode:fit; fill-color:red; stroke-mode:plain; stroke-width:1; stroke-color:black;");
//                n.addAttribute("ui.style", "shape:freeplane; size:20px;");
                graph.addNode("B");
                graph.addNode("C");
                graph.addEdge("AB", "A", "B");
                graph.addEdge("BC", "B", "C");
                graph.addEdge("CA", "C", "A");
 
                graph.display();
        }
}