
package mx.ipn.cic.piig.disc.algoritmos;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.graphstream.algorithm.Algorithm;
import static org.jocl.CL.*;

import org.jocl.*;
import org.graphstream.graph.*;

/**
 * A small JOCL sample.
 */
public class DijkstraAP_CL implements Algorithm
{
    public static String NODE_INDEX_ATT = "__NodeIndexAtt";
    public static int MY_LOCAL_WORKGROUP_SIZE=64;
    public String EDGE_WEIGHT_ATT = "weight";
    public void setAtributoDePeso( String peso ) { EDGE_WEIGHT_ATT = peso; }
    
    boolean useGPU = true;
    long devType = CL_DEVICE_TYPE_GPU;
    
    Graph graph;
    ArrayList<Node> nodos;
    float gMatrix[], dist[];
    
    /**
     * Realiza un alineamiento entre el número de núcleos que se utilizarán y el 
     * tamaño de la entrada. 
     * @param size tamaño de la entrada.
     * @param localSize número de núcleos que se utilizarán.
     * @return 
     */
    static int alignWorkGroupSize(int size, int localSize) {
        if( (size%localSize) != 0) {
            return size + localSize - (size % localSize);
        }

        return size;
    }

    /**
     * Inicializa los parámetros del algoritmo. Convierte el grafo graphstream en 
     * su representación matricial; en nodos se guarda el orden que tienen los nodos 
     * en la matriz.
     * @param g grafo sobre el que se ejecuta el algoritmo.
     */
    @Override
    public void init(Graph g) {
        graph = g;
        nodos = new ArrayList<>(graph.getNodeSet());
        nodos.sort((Node a, Node b) -> {
            return a.getId().compareTo(b.getId());
        });
        
        for(int i=0; i<nodos.size(); i++) {
            nodos.get(i).setAttribute(NODE_INDEX_ATT, i);
        }
        
        gMatrix = new float[nodos.size() * nodos.size()];
        for( int i=0; i<gMatrix.length; i++ ) { gMatrix[i] = -1; }
        graph.getEdgeSet().forEach((e) -> {
            int i = (int)e.getNode0().getAttribute(NODE_INDEX_ATT);
            int j = (int)e.getNode1().getAttribute(NODE_INDEX_ATT);
            float value = ((Double)e.getAttribute(EDGE_WEIGHT_ATT)).floatValue();
            
            gMatrix[nodos.size()*i + j] = value;
        });
        
        dist = new float[nodos.size() * nodos.size()];    
    }

    /**
     * Ejecuta el algoritmo de Dijkstra en GPU y si no se puede utiliza la versión CPU.
     */
    @Override
    public void compute() {
        try {
            if( useGPU )
                dijkstraAP_GPU(nodos.size(), gMatrix, dist, devType);
            else
                dijkstraAP_CPU(nodos.size(), gMatrix, dist);
        } catch(Exception e) {
            useGPU = false;
            dijkstraAP_CPU(nodos.size(), gMatrix, dist);
        }
    }
    
    public Node getNode(int i) {
        return nodos.get(i);
    }
    
    public void setGPU(boolean useGPU, long devType) {
        this.useGPU = useGPU;
        this.devType = devType != 0? devType : CL_DEVICE_TYPE_GPU;
    }
    
    /**
     * Devuelve el valor de la distancia entre los nodos dados.
     * @param a Nodo fuente.
     * @param b Nodo destino.
     * @return Distancia entre a y b.
     */
    public float getDistance(Node a, Node b) {
        int i = a.getAttribute(NODE_INDEX_ATT);
        int j = b.getAttribute(NODE_INDEX_ATT);
        
        return dist[nodos.size()*i + j];
    }
    
    /**
     * Devuelve el valor de la distancia entre los nodos dados.
     * @param i Nodo fuente.
     * @param j Nodo destino.
     * @return Distancia entre i y j.
     */
    public float getDistance(int i, int j) {
        return dist[nodos.size()*i + j];
    }
    
     /**
     * Calcula la distancia entre todas las parejas de nodos utilizando el algoritmo de
     * dijkstra para un grafo representado con una matriz.
     * @param nNodes número de nodos en el grafo.
     * @param graphMatrix matriz de nNodos*nNodos conteniendo el grafo.
     * @param dist matriz de nNodos*nNodos con la distancia entre cada pareje de nodos.
     */
    public static void dijkstraAP_CPU(int nNodes, float graphMatrix[], float dist[]) {
        for(int i=0; i<nNodes; i++) {
            dijkstra_CPU(i, nNodes, graphMatrix, dist);
        }
    }
    
    /**
     * Algoritmo de Dijkstra sobre un grafo descrito por una matriz.
     * @param source índice del nodo fuente.
     * @param nNodos número de nodos.
     * @param weight matriz con los pesos de las relaciones de los nodos (tamaño nNodos*nNodos).
     * @param distances matriz con las distancias calculadas (tamaño nNodos*nNodos).
     */
    public static void dijkstra_CPU(int source, int nNodos, float weight[], float distances[]) {
        boolean nodeVisited[] = new boolean[nNodos];
        int offset = nNodos*source;                                             // inicio de la fila que se va a calcular

        for( int i=0; i<nNodos; i++ ) {
            nodeVisited[i] = false;
            distances[offset + i] = weight[offset + i];
        }
        nodeVisited[source] = true;
        distances[offset + source] = 0;

        for( int i=0; i<nNodos; i++ ) {
            float minDist = Float.POSITIVE_INFINITY;
            int minNode = source;

            for( int j=0; j<nNodos; j++) {
                if( nodeVisited[j] ) {
                    continue;
                }
                if( distances[offset + j]>=0 && distances[offset + j]<minDist ) {
                    minDist = distances[offset + j];
                    minNode = j;
                }
            }

            nodeVisited[minNode] = true;

            for( int j=0; j<nNodos; j++) {
                float curWeight = weight[minNode*nNodos + j];
                if( curWeight<0 ) {
                    continue;
                }
                if( distances[offset + j]<0 ) {
                    distances[offset + j] = curWeight + minDist;
                }
                if( curWeight + minDist < distances[offset + j] ) {
                    distances[offset + j] = curWeight + minDist;
                }
            }
        }
    }

    /**
     * Implementación estática del algoritmo de Dijkstra para un grafo con representación
     * matricial utilizando GPU's.Calcula la distancia entre todas las parejas.
     * @param nNodes
     * @param graphMatrix
     * @param dist
     * @param deviceType
     * @throws Exception 
     */
    public static void dijkstraAP_GPU(int nNodes, float graphMatrix[], float dist[], final long deviceType) throws Exception {
        int N2 = nNodes*nNodes;
        String sourcefile = "dijkstra_opencl_kernel.cl";
        String clFunctionName = "dijkstra_float";
        Pointer nNodesPtr = Pointer.to(new int[]{nNodes});
        Pointer gMatrixPtr = Pointer.to(graphMatrix);
        Pointer distPtr = Pointer.to(dist);

        // Load the kernel source code into the array source_str
        String source_str = new String(Files.readAllBytes(Paths.get(sourcefile)));;
        
        // The platform, device type and device number
        // that will be used
        final int platformIndex = 0;
        final int deviceIndex = 0;
        
        // Enable exceptions and subsequently omit error checks in this sample
        CL.setExceptionsEnabled(true);

        // Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
        
        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        
        // Obtain a device ID 
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[deviceIndex];
        
        // Create an OpenCL context
        cl_context context = clCreateContext(
            contextProperties, 1, new cl_device_id[]{device}, 
            null, null, null);
        
        // Create a command queue
        cl_command_queue commandQueue =    clCreateCommandQueue(context, device, 0, null);
        
        // Create the program from the source code
        cl_program program = clCreateProgramWithSource(context, 1, new String[]{ source_str }, null, null);

        // Build the program
        clBuildProgram(program, 0, null, null, null, null);
        
        // Create the kernel
        cl_kernel kernel = clCreateKernel(program, clFunctionName, null);

        // Allocate the memory objects for the input and output data
        cl_mem memObjects[] = new cl_mem[3];
        memObjects[0] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_int, nNodesPtr, null);
        memObjects[1] = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, Sizeof.cl_float * N2, gMatrixPtr, null);
        memObjects[2] = clCreateBuffer(context, CL_MEM_WRITE_ONLY, Sizeof.cl_float * N2, null, null);

        // Set the arguments for the kernel
        clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObjects[0]));
        clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObjects[1]));
        clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memObjects[2]));
        
        // Set the work-item dimensions
        long global_work_size[] = new long[]{alignWorkGroupSize(nNodes, MY_LOCAL_WORKGROUP_SIZE)};
        long local_work_size[] = new long[]{MY_LOCAL_WORKGROUP_SIZE};
        
        // Execute the kernel
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, global_work_size, local_work_size, 0, null, null);
        
        // Read the output data
        clEnqueueReadBuffer(commandQueue, memObjects[2], CL_TRUE, 0, N2 * Sizeof.cl_float, distPtr, 0, null, null);
        
        // Release kernel, program, and memory objects
        clReleaseMemObject(memObjects[0]);
        clReleaseMemObject(memObjects[1]);
        clReleaseMemObject(memObjects[2]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }
}
