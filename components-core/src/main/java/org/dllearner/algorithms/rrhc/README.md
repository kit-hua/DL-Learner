# Monte Carlo Tree Search for DL-Learner

This algorithm is an extension of CELOE, inspired by Monte Carlo Tree Search (MCTS).

## Summary of the Algorithm
MCTS generally consists of four steps:
1. Selection
    - Selecting a node according to a heuristic. This is much like in CELOE.
    - If there is a node with the unique highest CELOE score, it is selected.
    - Otherwise the UCT score calculated by MCTS is used.
2. Expansion
    - The selected node is expanded.
    - This is the same as in CELOE.
3. Simulation
    - A simulation is started from one of the new leaf nodes.
    - Simulation steps are done until a stop condition is met.
    - A modified version of RhoDRDown is used to sample refinements.
4. Backpropagation
    - The result of a simulation is back-propagated from the leaf to the root in order to adjust the UCT score.
     - This is done in the form of the accuracy gain between the leaf and its parent.
    

## The implementation in Detail

The class MCTS implements the main algorithm.
As it is based on CELOE, it was convenient to reuse a lot of its code.
For this purpose, a few changes to the CELOE code have been made (changed a few properties and methods from _private_ to _protected_) which is what the class CELOE_MCTS is for, in order not to change the original codebase.

The **selection** is still mostly guided by CELOE. If the CELOE heuristic gives one node the unique best score, then that node is selected. Otherwise, UCT is considered. There are two ways to do this:
- Traverse the tree from the root, following the node with the highest UCT score
- From the nodes sharing the best CELOE score, select one with the best UCT score.

The Configuration option _uctTraverseWholeTree_ controls this behaviour. If it is set to true, then the tree will be traversed in this case.

For the **simulation** part of the algorithm, the class MCTSSimulator is used.
A simulation is started by calling the _simulate_ method with one of the search tree's nodes.
During the simulation, several _simulation steps_ are made.
In each step, the modified RhoDRDown (RhoDRDownMCTS) refinement operator is used to sample refinements.
The sampled concepts are then measured in terms of accuracy.
Now, one of these concepts is selected at random, weighted by their CELOE score and serves as input for the next step.
If the accuracy decreases between steps, or stays the same for a number of steps, the simulation is aborted.
For performance reasons it is recommended to use the _getFinalAccuracy_ method to get the accuracy of the final concept of a simulation.
This accuracy can then be used for adjusting the UCT score.

As mentioned above, MCTS uses another heuristic, which is implemented in the class MCTSHeuristic.
It is based on the "Upper Confidence Bound applied to Trees" (UCT) and aims to balance exploration and exploitation.
The results of the simulation are used to adjust the UCT score.
This is done by only considering the accuracy gain of the simulation's final concept with respect to the leaf that the simulation has been started from.
That accuracy gain is **back-propagated** through the tree, and added to each node's _total score_ in the heuristic, for all the nodes between the leaf and the root.
The better the total score of a node is, the better its UCT score will be.
On the other hand, there is also an exploration term influencing the heuristic.


The changes to RhoDRDown mentioned before are implemented in the method _refineSimulation_ of RhoDRDownMCTS.
They make sure that not all possible refinements are returned, but instead always one class of refinements is sampled at random.
This is necessary to reduce the amount of refinements considered in the simulation step, for reasons of performance.

The class MCTSUtil provides a few methods for sampling a node out of a collection of nodes, and using a given heuristic.