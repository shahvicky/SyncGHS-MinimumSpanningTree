
/**
 * @author shahvicky1992
 */
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

public class Node {

	final static Logger logger = Logger.getLogger(Node.class);
	static int myId;
	static String myHost;
	static int myPort;
	static int leaderId;
	static int parentId;
	static int level;
	static int numOfNodes;
	static boolean isOnCoreEdge = false;
	static ArrayList<Edge> basicEdges = new ArrayList<>();
	static ArrayList<Edge> branchEdges = new ArrayList<>();
	static ArrayList<Edge> rejectEdges = new ArrayList<>();
	static HashMap<Integer, ArrayList<String>> id2HostPortMap = new HashMap<>();
	static ConcurrentLinkedQueue<Message> buffer = new ConcurrentLinkedQueue<>(); // message queue

	public static void main(String[] args) throws IOException {

		InetAddress inetAddress = InetAddress.getLocalHost();
		myHost = inetAddress.getHostName();

		if (args.length > 0) {
			myId = Integer.parseInt(args[0]);
			myHost = "localhost";
		}
		FileParser parser = new FileParser();
		id2HostPortMap = parser.readFile(myId, myHost);
		myPort = Integer.parseInt(id2HostPortMap.get(myId).get(1));

		//sorting the basic edges
		Collections.sort(basicEdges);
		//initially, myId = leaderId for each node
		leaderId = myId;

		Listener listener = new Listener(myPort);
		Thread thread = new Thread(listener);
		thread.start();
		
		SyncGHS ghs = new SyncGHS(numOfNodes);
		ghs.constructMST();
		
		

	}
}
