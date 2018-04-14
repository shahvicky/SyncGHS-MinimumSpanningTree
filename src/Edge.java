
enum Edge_Type {
	BASIC, BRANCH, REJECTED
}


public class Edge {

	int weight;
	int minId;
	int maxId;
	Edge_Type edgeType;
	
	public Edge() {
		this.edgeType = Edge_Type.BASIC;
	}
	
	public Edge(int weight, int minId, int maxId) {
		this.weight = weight;
		this.minId = minId;
		this.maxId = maxId;
		this.edgeType = Edge_Type.BASIC;
	}
	
	public Edge(int weight, int minId, int maxId, Edge_Type edgeType) {
		this.weight = weight;
		this.minId = minId;
		this.maxId = maxId;
		this.edgeType = edgeType;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int getMinId() {
		return minId;
	}

	public void setMinId(int minId) {
		this.minId = minId;
	}

	public int getMaxId() {
		return maxId;
	}

	public void setMaxId(int maxId) {
		this.maxId = maxId;
	}

	public Edge_Type getEdgeType() {
		return edgeType;
	}

	public void setEdgeType(Edge_Type edgeType) {
		this.edgeType = edgeType;
	}

	@Override
	public String toString() {
		return "Edge [weight=" + weight + ", minId=" + minId + ", maxId=" + maxId + ", edgeType=" + edgeType + "]";
	}
	
	
	
}
