/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphstream.test;

import javax.swing.JFileChooser;
import java.awt.*;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceFactory;
 
import java.io.IOException;
import java.util.Iterator;
import org.graphstream.stream.file.FileSourceDOT;
import org.graphstream.ui.view.Viewer;
 
public class TutorialFileSource {
 
  public static void main(String ... args) {
      //System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
      FileDialog dialog1=new FileDialog((Frame)null,"Abrir...",FileDialog.LOAD);
      dialog1.setVisible(true);
      
      
      String dir=dialog1.getDirectory();
      String file=dialog1.getFile();
      String str=dir+file;
      
      Graph graph = new DefaultGraph("g");
      graph.addAttribute("ui.stylesheet", styleSheet);
      graph.setAttribute("ui.antialias");
      graph.setAutoCreate(true);
      graph.setStrict(false);
      graph.display();

      FileSource fs = null; 
      try {
          fs = FileSourceFactory.sourceFor(str);
          fs.addSink(graph);
          fs.begin(str);
          while (fs.nextEvents()) {
              // Optionally some code here ...
              sleep(5);
          }
          fs.end();
          fs.removeSink(graph);
      } catch( IOException e) {
          e.printStackTrace();
      } 
      
//      FileSource fs = new FileSourceDOT();
//    fs.addSink(graph);
//    try{
//        fs.readAll(str);
//    } catch( IOException e) {
//        System.err.println(fs);
//      e.printStackTrace();
//    } finally {
//      fs.removeSink(graph);
//    }
    
    
    
      explore(graph.getNode("n000"));
    
//    for( Node n:graph) {
//        n.addAttribute("ui.label", n.getId());
//    }
    
  }
    public static void explore(Node source) {
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

    protected static String styleSheet =
        "node {" +
        "   fill-color: black;" +
        "}" +
        "node.marked {" +
        "   fill-color: red;" +
        "}";
}