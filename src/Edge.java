
/**
 * @author shahvicky1992
 */
enum Edge_Type {
	BASIC, BRANCH, REJECTED
}

public class Edge implements Comparable<Edge> {

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

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Edge e) {
		int temp = Integer.compare(weight, e.weight);
		if(temp == 0) {
			temp = Integer.compare(getMinId(), e.getMinId());
			return temp == 0 ? Integer.compare(getMaxId(), e.getMaxId()) : temp;
		} else {
			return temp;
		}
	}
	
	
	
}
