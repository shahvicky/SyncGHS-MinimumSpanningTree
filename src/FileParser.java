
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class FileParser{

	int numOfNodes;
	
	public void readFile(){
		File file = new File("C:\\Users\\shahvicky1992\\ProjectWorkspace\\Distributed\\SyncGHS\\src\\config1.txt");
		try( BufferedReader reader = new  BufferedReader(new FileReader(file)) ) {
			String line;
			/*read the number of nodes in the cofinguration*/
			while((line = reader.readLine()) != null) {
				if(!isLineValid(line))
					continue;
				numOfNodes = Integer.parseInt(line);
				Node.numOfNodes = numOfNodes;
				break;
			}
			System.out.println(numOfNodes);
			/*read the nodes id and the host and post info and store it as a hashmap
			 * as { id = [host, port]}*/
			
			HashMap<Integer, ArrayList<String>> id2HostPostMap = new HashMap<>();
			while((line = reader.readLine()) != null && numOfNodes>0) {
				if(!isLineValid(line))
					continue;
				String[] temp = line.split("\\s+");
				id2HostPostMap.put(Integer.parseInt(temp[0]), new ArrayList<String>());
				id2HostPostMap.get(Integer.parseInt(temp[0])).add(temp[1]);
				id2HostPostMap.get(Integer.parseInt(temp[0])).add(temp[2]);
				numOfNodes--;
			}
			
			System.out.println(id2HostPostMap.toString());
			ArrayList<Edge> basicEdges = new ArrayList<>();
			while((line = reader.readLine()) != null) {
				if(!isLineValid(line))
					continue;
				String[] edgeLine = null;
				edgeLine = line.split("\\s+");
				String edgeVertices = edgeLine[0].split("[\\(\\)]")[1];
				String idPair[]=edgeVertices.split(",");
				
				int weight= Integer.parseInt(edgeLine[1]);
				int minId = Integer.parseInt(idPair[0]);
				int maxId = Integer.parseInt(idPair[1]);
				
				basicEdges.add(new Edge(weight, minId, maxId));
			}
			System.out.println(basicEdges.toString());
			
			
		} catch (IOException e) {
			System.out.println("File not found");
			//e.printStackTrace();
		}
		
	}
	
	private boolean isLineValid(String line) {
		return line.length() > 0 && !line.startsWith("#");
	}
	
	
	
	
}
