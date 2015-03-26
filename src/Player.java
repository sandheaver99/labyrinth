import java.util.*;
import java.io.*;
import java.math.*;

class Player
{
	private final Scanner in = new Scanner(System.in);
	private final int NEIGHBOUR_LIMIT = 4;
	private int rows;
	private int columns;
	private int alarmDelay; //(in rounds)
	
	private int currentX;
	private int currentY;
	private int turnNumber = 0;
	
	private List<Node> maze = new ArrayList<Node>();		
	private Node startingPosition;
	private Node currentPosition;
	private Node controlRoom;
	private Node target;
	private boolean controlVisited = false;

    public static void main(String args[])
    {
		new Player().go();
	}
	
	private void go()
	{
		initialise();
		setNodeNeighbours();
		play();
	}
	
	private void initialise()
	{
        rows = in.nextInt(); // number of rows.
        columns = in.nextInt(); // number of columns.
        alarmDelay = in.nextInt(); // number of rounds between the time the alarm countdown is activated and the time the alarm goes off.
			
		System.err.println("Number of nodes in maze is " + (rows * columns));
		
		//instantiate initial grid of nodes, setting all nodes to 'not scanned' type
		for(int row = 0; row < rows; row++)
		{
			for(int col = 0; col < columns; col++)
			{			
				int id = col + (row*columns);
				maze.add(new Node("?", col, row, id));				
			}
		}
	}
	
	private void setNodeNeighbours()
	{
		//neighbours will be nodes with valid id's offset by [-columns, +columns, -1, & +1]
		int[] offsets = {-columns, +columns, -1, 1};
		for(Node n: maze)
		{
			for(int o: offsets)
			{
				int candidateId = n.getId() + o;
				//if the candidate neighbour is outside the maze, dismiss this candidate
				if(candidateId < 0 || candidateId > (rows*columns)-1)
				{
					continue;
				}
				//if the current node is on the left edge of the maze and the candidate neighbour is 'to the left', dismiss this candidate
				if(o == -1 && (n.getId() % columns) == 0)
				{
					continue;
				}
				//if the current node is on the right edge of the maze and the candidate neighbour is 'to the right', dismiss this candidate
				if(o == 1 && (n.getId() % columns) == (columns-1))
				{
					continue;
				}
				//add candidate to the node's neighbours list
				Node neighbour = maze.get(candidateId);
				n.addNeighbour(neighbour);
					
				//check for no false neighbours
				if(n.getNeighbours().size() > NEIGHBOUR_LIMIT)
				{
					System.err.println("Error: Node " + n.getId() + " has " + n.getNeighbours().size() + " neighbours.");	
				}		
			}
		}			
	}
	
	private void play()
	{
        // game loop
        while (true)
        {
			turnNumber++;
            readMaze();
            chooseTarget();
            List<Node> path = findShortestPath();
            String message = getMessage(path.get(0));
            System.out.println(message); // Kirk's next move (UP DOWN LEFT or RIGHT).
            drain();
        }
    }
    
    private void readMaze()
    {
		currentY = in.nextInt(); // row where Kirk is located.
		currentX = in.nextInt(); // column where Kirk is located.
		if(turnNumber == 1)
		{
			//if this is the first turn, set the starting point to the node at the current location of Kirk. 
			//this is double checked within the row update loop below, by checking for type "T".
			startingPosition = maze.get((currentY * columns) + currentX);
		}
		
		int mazeIndex = 0;
		for (int i = 0; i < rows; i++)
        {
            String rowCharacters = in.next(); // C of the characters in '#.TC?' (i.e. one line of the ASCII maze).
            String[] characters = rowCharacters.split("");
            for(String c: characters)
            {				
				//note the location of the control room , if present, to avoid needing to search for it later.  
				if(c.equals("C"))
				{
					controlRoom = maze.get(mazeIndex);
				}
				if(c.equals("T"))
				{
					if(!startingPosition.equals(maze.get(mazeIndex)))
					{
						System.err.println("Error: The starting position has been set incorrectly.");
					}
				}
				//update the location's node's type
				maze.get(mazeIndex).setType(c);
				mazeIndex++;
			}				
        }
		// find current node
        currentPosition = maze.get((currentY * columns) + currentX);
        System.err.println("My calculated current position is [" + currentPosition.getX() + "," + currentPosition.getY() + "]");
        if(currentPosition.getType().equals("C"))
        {
			controlVisited = true;
		}
	}
	
	private void chooseTarget()
	{
		System.err.println("Beginning maze flood routine ...");
		floodMaze();
		System.err.println("Maze flood routine completed");
		
		//if control is visited, target = entry and path is known and accessible
		if(controlVisited)
		{
			target= startingPosition;
			return;
		}
		
		//if control is not visited but already the target, we make no change to the target. 
		else if(controlRoom != null) 
		{
			if(target.equals(controlRoom))
			{
				return;
			}
		}
		
		/*
		 * if we reach this point then the control room has not been found in previous turns and evaluated, so we must
		 * in any event flood the maze to establish the accessibility of every known node, even if the control room
		 * is known on this turn, since in any event, we will be updating the target.  
		 * 
		 */
		 
		
		
		//If control is known (and accessible), target = control
		if(controlRoom != null) 
		{
			if(controlRoom.isFlooded())
			{
				target = controlRoom;
			}
		}
		
		/*
		 * otherwise, target = 'center' of unknown area 
		 * each unknown tile will have an x and y so can take avg of these (N) , 
		 * & search around this point for accessible (flooded) Node (N/?)
		 * 
		 */
		 
		 else
		 {
			 System.err.println("Searching for furthest known node ....");
			 target = findFurthestKnownNode();
			 System.err.println("Search completed");
		 }
		 
		 System.err.println("Target is [" + target.getX() + "," + target.getY() + "] and is of type " + target.getType());
		 System.err.println("Target is flooded? : " + target.isFlooded());
	}		
	
	
	private void floodMaze()
	{
		//recursive implementation of floodfill (within the Node class) can be used if maze has small number of nodes
		if(maze.size() < 1000)
		{
			currentPosition.flood();
		}
		
		//otherwise use non-recursive stack algorithm to floodfill
		else
		{
			currentPosition.setFlooded(true);
			Stack<Node> floodStack = new Stack<Node>();
			floodStack.push(currentPosition);
			
			while(floodStack.size() > 0)
			{
				Node n = floodStack.pop();
				for(Node edge: n.getNeighbours())
				{
					if(!edge.isFlooded() && !n.getType().equals("#"))
					{
						edge.setFlooded(true);
						floodStack.push(edge);
					}
				}
			}					
		}		
	}	
	
	private Node findFurthestKnownNode() //from startingPoint
	{			
		Node furthest = currentPosition;
		int largestManhatten = 0;
		
		for(Node n: maze)
		{
			String type = n.getType();
			if(!type.equals("?") && !type.equals("#") && n.isFlooded())
			{
				int manhattenX = Math.abs(n.getX() - startingPosition.getX());
				int manhattenY = Math.abs(n.getY() - startingPosition.getY());
				int manhatten = manhattenX + manhattenY;
				if(manhatten > largestManhatten)
				{
					largestManhatten = manhatten;
					furthest = n;
				}
			}
		}		
				
		return furthest;
	}
	
	private List<Node> findShortestPath()
	{
		/* 
		 * uses A* algorithm to find shortest path from current position to target
		 * (a route is assumed to exist because target is flooded,
		 *  although "?" may decieve)
		 */
		
		List<Node> open = new ArrayList<Node>();
		List<Node> closed = new ArrayList<Node>();
		
		// Add the starting square (or node) to the open list.
		
		open.add(currentPosition); 
		
		while(open.size() > 0) 
		{
			/*
			 * Look for the lowest F cost square on the open list. 
			 * We refer to this as the subjectNode.
			 */
			 
			Node subjectNode = open.get(0); 
			
			// Switch it to the closed list.
			
			open.remove(subjectNode);
			closed.add(subjectNode);
			
			//Stop when you: Add the target square to the closed list, in which case the path has been found 
			if(subjectNode.equals(target))
			{
				break;
			}
			
			//For each of the 4 nodes adjacent (NSEW) to this subject node …
			
			for(Node neighbour: subjectNode.getNeighbours())
			{
				//If it is not walkable or if it is on the closed list, ignore it. 
				 
				if(neighbour.isFlooded() && !closed.contains(neighbour))
				{
					// Otherwise do the following.
					//If it isn’t on the open list, add it to the open list.
					if(!open.contains(neighbour))
					{
						open.add(neighbour);
						//Make the subject node the parent of this neighbouring node
						neighbour.setParent(subjectNode);
						//Record the F, G, and H costs of the square. 
						int h = neighbour.calcHCost(target);
						neighbour.setHValue(h);
						neighbour.setGCost(subjectNode.getGCost() + neighbour.getBaseCost());
						neighbour.setFValue();
					}
					
					//If it is on the open list already, 
					else
					{
						/*
						 * check to see if this path to that node is better,
						 * using G cost as the measure. A lower G cost means that this is a better path.
						 * 
						 */
						 if((subjectNode.getGCost() + neighbour.getBaseCost()) < neighbour.getGCost())
						 {
							 /*
							  * If so, change the parent of the neighbour to the subjectNode,
							  * and recalculate the G and F scores of the node.
							  * If keeping your open list sorted by F score,
							  * resort the list to account for the change.
							  */
							  neighbour.setParent(subjectNode);
							  neighbour.setGCost(subjectNode.getGCost() + neighbour.getBaseCost());
							  neighbour.setFValue();
						 }							  
					}
						 
				}				
			}
			//sort by shortest FValue
			Collections.sort(open);
		}	
		if(!closed.contains(target))
		{
			System.err.println("Cannot find a path to the target square");
		}
		
		/*
		 * Save the path. Working backwards from the target node, go from each node to
		 * its parent node until you reach the current position. That is your path. 
		 */
		 List<Node> shortestPath = new ArrayList<Node>();		 
		 shortestPath.add(target);
		 while(!shortestPath.contains(currentPosition))
		 {
			//get the parent of the last item added to the shortestPath and add it to the shortestPath
			 shortestPath.add(shortestPath.get(shortestPath.size()-1).getParent());
		 }
		 //reverse the path
		 Collections.reverse(shortestPath);
		 //remove the current position, which will now be at the start of the list
		 shortestPath.remove(currentPosition);
		 
		 System.err.println("My shortest path to the target is: ");
		 for(Node n:shortestPath)
		 {
			System.err.print("[" + n.getX() + ", " + n.getY() + "], ");			 
		 }
		 System.err.println("");	
		 
		 return shortestPath;	  					
	}
	
	private String getMessage(Node next)
	{
		int delta = next.getId() - currentPosition.getId();
		switch(delta)
		{
			case -1: return "LEFT";
			case  1: return "RIGHT";
			default: if(delta > 0)
			{
				return "DOWN";
			}
			else
			{
				return "UP";
			}
		}
	}
	
	private void drain()
	{
		for(Node n: maze)
		{
			n.setFlooded(false);
		}
	}
			
	
		
}

class Node implements Comparable<Node>
{
	/*
	 * In terms of Graph theory, A Node is a Vertex with Edges represented by Nodes in its 'neighbours' list.
	 * 
	 * The <String> type of the node matches the Maze input characters. 
	 * The character # represents a wall, 
	 * the letter . represents a hollow space, 
	 * the letter T represents your starting position,
	 * the letter C represents the control room
	 * and the character ? represents a cell that you have not scanned 	 
	 */
	
	private String type;
	private final int baseCost = 1; //the movement cost of the node itself (always 1 in this problem)
	private final int x; //x Co-ordinate of the node
	private final int y; //y Co-ordinate of the node
	private final int id; //the index of the node in the master list of the grid
	
	private int fValue;	//gCost + hValue
	private int gCost; //the movement cost to move from the starting point A to this node on the grid, following the path generated to get there. 
	private int hValue; //the estimated movement cost to move from this node to the final target destination.
	private boolean flooded = false; // set to true if impacted by a call to floodfill
	private Node parent;
	List<Node> neighbours = new ArrayList<Node>();
	
	public Node(String type, int x, int y, int id)
	{
		this.type = type;
		this.x = x;
		this.y = y;
		this.id = id;
	}
	
	public void flood()
	{
		/*
		 * recursive implementation of floodfill routine, 
		 * but not used due to potential stack overflow errors
		 * (up to 20,000 nodes are possible in the problem and
		 * therefore up to 20,000 recursive calls would be made		 * 
		 */
		 
		 flooded = true;
		 for(Node n: neighbours)
		 {
			 if(!n.isFlooded() && !n.getType().equals("#"))
			 {
				 n.flood();
			 }
		 }
	}
	
	//getters for final instance variables		
	public int getBaseCost()
	{
		return baseCost;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getId()
	{
		return id;
	}
	
	//getters for non-final instance variables
	public String getType()
	{
		return type;
	}
	
	public int getFValue()
	{
		return fValue;
	}
	
	public int getGCost()
	{
		return gCost;
	}
	
	public int getHValue()
	{
		return hValue;
	}
	
	public boolean isFlooded()
	{
		return flooded;
	}
	
	public Node getParent()
	{
		return parent;
	}
	
	//setters for non-final instance variables
	public void setType(String t)
	{
		type = t;
	}
	
	public void setFValue()
	{
		fValue = getGCost() + getHValue();
	}
	
	public void setGCost(int g)
	{
		gCost = g;
	}
	
	public void setHValue(int h)
	{
		hValue = h;
	}
	
	public void setFlooded(boolean b)
	{
		flooded = b;
	}
	
	public void setParent(Node n)
	{
		parent = n;
	}
	
	//getter and setter for neighbours list
	public List<Node> getNeighbours()
	{
		return neighbours;
	}
	
	public void addNeighbour(Node n)
	{
		neighbours.add(n);
	}
	
	//must overide compareTo method (for open list sorting purposes)
	@Override
	public int compareTo(Node n)
    {
        return fValue > n.getFValue() ? +1 : fValue < n.getFValue() ? -1 : 0;        
    }
    
    public int calcHCost (Node target)
    {
		//calculate manhatten costs
		int manX = Math.abs(getX() - target.getX());
		int manY = Math.abs(getX() - target.getX());
		return manX + manY;
	}		
}
