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
	private Node controlRoom;
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
                       

            System.out.println("RIGHT"); // Kirk's next move (UP DOWN LEFT or RIGHT).
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
			startingPosition = maze.get((currentY * currentX) + currentX);
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
	}
	
	private void chooseTarget()
	{
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
	
	public void setFValue(int f)
	{
		fValue = f;
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
}
