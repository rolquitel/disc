
__kernel void dijkstra_float(__global int *nNodesPtr, __global float *weight, __global float *distances) {
    int nodeVisited[8192];
    // Get the index of the current element
    int sourceNode = get_global_id(0);
    int nNodes = *nNodesPtr;
    
    if( sourceNode >= nNodes ) return;
    
    __global float *distanceTo = distances + nNodes*sourceNode;
    
    for( int i=0; i<nNodes; i++ ) {
        nodeVisited[i] = 0;
        distanceTo[ i] = weight[ nNodes*sourceNode + i ];
    }
    
    nodeVisited[sourceNode] = 1;
    distanceTo[sourceNode] = 0;
    
    for(int i=1; i<nNodes; i++ ) {
        float minDist = 1e10;
        int minNode = sourceNode;
        
        for( int j=0; j<nNodes; j++) {
            if( nodeVisited[j] ) {
                continue;
            }
            if( distanceTo[j]>=0 && distanceTo[j]<minDist ) {
                minDist = distanceTo[j];
                minNode = j;
            }
        }
        
        nodeVisited[minNode] = 1;
        
        for( int j=0; j<nNodes; j++) {
            float curWeight = weight[minNode * nNodes + j];
            if( curWeight < 0 ) {
                continue;
            }
            if( distanceTo[j]<0 ) {
                distanceTo[j] = curWeight + minDist;
            }
            if( curWeight + minDist < distanceTo[j] ) {
                
                distanceTo[j] = curWeight + minDist;
            }
        }
    }
}

__kernel void compute_generality(const int nNodes, const __global float *distances, __global float *generality) {
    int curNode = get_global_id(0);
    float from = 0.0;
    float to = 0.0;
    
    if( curNode >= nNodes ) return;
    
    for( int i=0; i<nNodes; i++) {
        from += distances[nNodes*curNode + i];
        to   += distances[nNodes*i + curNode];
    }
    
    generality[curNode] = from / to;
}
