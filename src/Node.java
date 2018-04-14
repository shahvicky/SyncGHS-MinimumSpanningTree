
/**
 * @author shahvicky1992
 */
import java.util.ArrayList;
import java.util.List;

public class Node {

	int myId;
	int leaderId;
	int level;
	static int numOfNodes;
	ArrayList<Edge> basicEdges;
	ArrayList<Edge> branchEdges;
	ArrayList<Edge> rejectEdges;
}
