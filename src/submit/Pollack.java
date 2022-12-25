package submit;

import java.util.HashSet;
import java.util.List;

import graph.FindState;
import graph.Finder;
import graph.FleeState;
import graph.Node;
import graph.NodeStatus;

/** A solution with find-the-Orb optimized and flee getting out as fast as possible. */
public class Pollack extends Finder {
    HashSet<Long> visited= new HashSet<>(); // Used to keep track of which tiles we have visited
    boolean orbFound= false;

    /** Get to the orb in as few steps as possible. <br>
     * Once you get there, you must return from the function in order to pick it up. <br>
     * If you continue to move after finding the orb rather than returning, it will not count.<br>
     * If you return from this function while not standing on top of the orb, it will count as <br>
     * a failure.
     *
     * There is no limit to how many steps you can take, but you will receive<br>
     * a score bonus multiplier for finding the orb in fewer steps.
     *
     * At every step, you know only your current tile's ID and the ID of all<br>
     * open neighbor tiles, as well as the distance to the orb at each of <br>
     * these tiles (ignoring walls and obstacles).
     *
     * In order to get information about the current state, use functions<br>
     * currentLoc(), neighbors(), and distanceToOrb() in FindState.<br>
     * You know you are standing on the orb when distanceToOrb() is 0.
     *
     * Use function moveTo(long id) in FindState to move to a neighboring<br>
     * tile by its ID. Doing this will change state to reflect your new position.
     *
     * A suggested first implementation that will always find the orb, but <br>
     * likely won't receive a large bonus multiplier, is a depth-first search. <br>
     * Some modification is necessary to make the search better, in general. */
    @Override
    public void find(FindState state) {
        // TODO 1: Walk to the orb
        optimizedDFS(state);

    }

    /** Implemented a DFS walk to find the orb. No optimization used Written in a separate method
     * and called in method find. Written using the basic dfs walk algorithm to wawlk through the
     * graph and find the orb */

    public void dfsWalk(FindState state) {
        if (state.distanceToOrb() == 0) {
            orbFound= true;
            return;
        }
        long curr= state.currentLoc();
        visited.add(curr);
        for (NodeStatus n : state.neighbors()) {
            long node= n.getId();
            if (!visited.contains(node)) {
                state.moveTo(node);
                dfsWalk(state);
                if (orbFound) { return; }

                if (!orbFound) {
                    state.moveTo(curr);
                }

            }

        }

    }

    /** An optimized version of method dfsWalk so Pollack can find the orb more efficiently Uses a
     * min-heap to prioritize the neighbors that are closer to the orb. Uses a greedy algorithm so
     * it is possible to Pollack doesn't always take the best route */
    public void optimizedDFS(FindState state) {
        if (state.distanceToOrb() == 0) {
            orbFound= true;
            return;
        }

        if (!orbFound) {
            long curr= state.currentLoc();
            visited.add(curr);

            Heap<NodeStatus> adj= new Heap<>(true);
            for (NodeStatus n : state.neighbors()) {
                adj.insert(n, n.getDistanceToTarget());
            }

            while (adj.size() != 0) {
                long pos= adj.poll().getId();
                if (!visited.contains(pos)) {
                    state.moveTo(pos);
                    optimizedDFS(state);
                    if (orbFound) { return; }
                    state.moveTo(curr);
                }
            }
        }

    }

    /** Get out the cavern before the ceiling collapses, trying to collect as <br>
     * much gold as possible along the way. Your solution must ALWAYS get out <br>
     * before steps runs out, and this should be prioritized above collecting gold.
     *
     * You now have access to the entire underlying graph, which can be accessed <br>
     * through FleeState state. <br>
     * currentNode() and exit() will return Node objects of interest, and <br>
     * allsNodes() will return a collection of all nodes on the graph.
     *
     * Note that the cavern will collapse in the number of steps given by <br>
     * stepsLeft(), and for each step this number is decremented by the <br>
     * weight of the edge taken. <br>
     * Use stepsLeft() to get the steps still remaining, and <br>
     * moveTo() to move to a destination node adjacent to your current node.
     *
     * You must return from this function while standing at the exit. <br>
     * Failing to do so before steps runs out or returning from the wrong <br>
     * location will be considered a failed run.
     *
     * You will always have enough steps to flee using the shortest path from the <br>
     * starting position to the exit, although this will not collect much gold. <br>
     * For this reason, using Dijkstra's to plot the shortest path to the exit <br>
     * is a good starting solution
     *
     * Here's another hint. Whatever you do you will need to traverse a given path. It makes sense
     * to write a method to do this, perhaps with this specification:
     *
     * // Traverse the nodes in moveOut sequentially, starting at the node<br>
     * // pertaining to state <br>
     * // public void moveAlong(FleeState state, List<Node> moveOut) */
    @Override
    public void flee(FleeState state) {
        // TODO 2. Get out of the cavern in time, picking up as much gold as possible.
        goldCollect(state);
    }

    /** Implements the shortest path algorithm to allow Pollack to escape the cavern as fast as
     * possible, disregarding the coins. Djikstra's Algorithm */

    public void abortMission(FleeState state) {
        List<graph.Node> route= Path.shortestPath(state.currentNode(), state.exit());
        for (graph.Node n : route) {
            if (n != state.currentNode()) {
                state.moveTo(n);
            }
        }
    }

    /** Uses a max-heap to prioritize the coins that carry the most value with distance to the coin
     * factored in as well. Uses Djikstra's shortest-path algorithm to locate said coins. Then when
     * the total number of steps remaining is equal to the number of steps needed to escape the
     * cavern we flee the cavern. */
    public void goldCollect(FleeState state) {
        while (state.stepsLeft() >= sumExit(state)) {
            Node n= pollHeap(state);
            toCoin(state, n);
        }
        abortMission(state);

    }

    public void altGoldCollect(FleeState state) {
        Heap<Node> optCoins= new Heap<>(false); // Used for coins weights
        Node curr= state.currentNode();
        for (Node n : state.allNodes()) {
            int dist= distanceTo(curr, n);
            int goldAmt= n.getTile().gold();
            if (dist != 0 || goldAmt != 0) {
                optCoins.insert(n, n.getTile().gold() / dist);
            }
        }
        while (optCoins.size() > 0) {
            Node m= optCoins.poll();
            if (state.stepsLeft() >= altSumExit(state, m)) {
                toCoin(state, m);
            }

        }
        abortMission(state);

    }

    /** Uses Djikstra's shortest-path to find the shortest path from the current location to the
     * coin we poll from our maxheap */
    public void toCoin(FleeState state, Node m) {
        List<graph.Node> route= Path.shortestPath(state.currentNode(), m);
        for (graph.Node n : route) {
            if (n != state.currentNode()) {
                state.moveTo(n);
            }
        }

    }

    /** Method develops the heap for the coins and polls the highest priority. This gets called
     * repeatedly so we keep having a new heap at each new position when the call is needed */
    public Node pollHeap(FleeState state) {
        Heap<Node> optCoins= new Heap<>(false); // Used for coins weights
        Node curr= state.currentNode();
        for (Node n : state.allNodes()) {
            int dist= distanceTo(curr, n);
            int goldAmt= n.getTile().gold();
            if (dist != 0 || goldAmt != 0) {
                optCoins.insert(n, n.getTile().gold() / dist);
            }

        }
        return optCoins.poll();

    }

    /** Method that keep track of the number of steps it takes to leave the cavern and get to a
     * desired tile */
    public int sumExit(FleeState state) {
        Node n= pollHeap(state);
        int i= distanceTo(state.currentNode(), n) + distanceTo(n, state.exit());
        return i;
    }

    public int altSumExit(FleeState state, Node n) {
        int i= distanceTo(state.currentNode(), n) + distanceTo(n, state.exit());
        return i;
    }

    /** This methods enumerates the shortest path to find the length of edges through out our
     * cavern */
    public int distanceTo(Node m, Node n) {
        int dist= 0;
        List<graph.Node> path= Path.shortestPath(m, n);
        for (int i= 0; i < path.size() - 1; i++ ) {
            dist+= path.get(i).edge(path.get(i + 1)).length();
        }
        return dist;

    }

}
