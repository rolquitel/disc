/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphstream.test;

import org.graphstream.ui.view.ViewerListener;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.*;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceFactory;
import java.io.IOException;
import java.util.Iterator;
import org.graphstream.stream.file.FileSourceDOT;
import javax.swing.JFileChooser;
import java.awt.*;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.DorogovtsevMendesGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.algorithm.generator.GridGenerator;
import org.graphstream.algorithm.generator.RandomEuclideanGenerator;
import org.graphstream.algorithm.generator.RandomGenerator;
import org.graphstream.algorithm.generator.WattsStrogatzGenerator;
import org.graphstream.ui.layout.HierarchicalLayout;


public class Clicks implements ViewerListener {
    protected boolean loop = true;
    
    protected static String styleSheet =
        "node {" +
        "   fill-color: black;" +
        "}" +
        "node.marked {" +
        "   fill-color: red;" +
        "}";
    Graph graph;
 
    public static void main(String args[]) {
        new Clicks();
    }
    
    public Clicks() {
        graph = new SingleGraph("Clicks");
//        graph.addAttribute("ui.stylesheet", "url('file:///Users/rolando/Google Drive/programacion/java/GraphStream/src/graphstream/graph.css')");
        graph.addAttribute("ui.antialias");
        graph.setAutoCreate(true);
        graph.setStrict(false);
        
        Viewer viewer = graph.display();
        //viewer.disableAutoLayout();
        //viewer.enableAutoLayout(new org.graphstream.ui.layout.Eades84Layout());
        
//        Generator gen = new RandomGenerator(5);
//        Generator gen = new DorogovtsevMendesGenerator();
//        Generator gen = new BarabasiAlbertGenerator(3);
//        Generator gen = new GridGenerator(true, true);
//        Generator gen = new WattsStrogatzGenerator(75, 2, 0.5);
        Generator gen = new RandomEuclideanGenerator();
        gen.addSink(graph);
        gen.begin();
        for(int i=0; i<1000; i++) {
            gen.nextEvents();
//            sleep(5);
        }
//        while(gen.nextEvents()) { sleep(5); }
        gen.end();
      
        // We do as usual to display a graph. This
        // connect the graph outputs to the viewer.
        // The viewer is a sink of the graph.
        //Graph graph = new SingleGraph("Clicks");
        
        
 
        // The default action when closing the view is to quit
        // the program.
        //viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);
 
        // We connect back the viewer to the graph,
        // the graph becomes a sink for the viewer.
        // We also install us as a viewer listener to
        // intercept the graphic events.
        ViewerPipe fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(this);
        fromViewer.addSink(graph);
 
        // Then we need a loop to do our work and to wait for events.
        // In this loop we will need to call the
        // pump() method before each use of the graph to copy back events
        // that have already occurred in the viewer thread inside
        // our thread.
 
        int i = 0;
        while(loop) {
            fromViewer.pump(); // or fromViewer.blockingPump(); in the nightly builds
 
            // here your simulation code.
 
            // You do not necessarily need to use a loop, this is only an example.
            // as long as you call pump() before using the graph. pump() is non
            // blocking.  If you only use the loop to look at event, use blockingPump()
            // to avoid 100% CPU usage. The blockingPump() method is only available from
            // the nightly builds.
        }
    }
 
    public void viewClosed(String id) {
        loop = false;
    }
 
    public void buttonPushed(String id) {
        System.out.println("Button pushed on node "+id);
    }
 
    public void buttonReleased(String id) {
        System.out.println("Button released on node "+id);
        explore(graph.getNode(id));
    }
    
    public void explore(Node source) {
        Iterator<? extends Node> k = source.getBreadthFirstIterator();

        while (k.hasNext()) {
            Node next = k.next();
            next.setAttribute("ui.class", "marked");
            sleep(5);
        }
    }

    protected static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (Exception e) {}
    }
}