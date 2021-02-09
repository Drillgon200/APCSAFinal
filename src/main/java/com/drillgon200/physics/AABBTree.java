package com.drillgon200.physics;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.drillgon200.shooter.util.Vec3f;

public class AABBTree<T extends TreeObject<T>> {
	//https://box2d.org/files/ErinCatto_DynamicBVH_GDC2019.pdf
	
	private Node<T> root;
	private List<Node<T>> invalids = new ArrayList<>();
	private List<Node<T>> collidingPairs = new ArrayList<>();
	private float margin;
	
	public AABBTree(float margin){
		this.margin = margin;
	}
	
	//Catto said "this leaves out several important optimizations", but I can't think of any, and I can't find the ray cast code in box2d
	//after a minute of searching. I can only hope he means SIMD and there's not something super obvious I'm missing.
	public RayTraceResult rayCast(Vec3f point1, Vec3f point2){
		if(root == null){
			return new RayTraceResult();
		}
		Vec3f direction = point2.subtract(point1);
		RayTraceResult bestResult = new RayTraceResult();
		Stack<Node<T>> stack = new Stack<>();
		stack.push(root);
		while(!stack.isEmpty()){
			Node<T> n = stack.pop();
			if(n.box.rayIntercepts(point1, direction) == null){
				continue;
			}
			if(n.isLeaf){
				RayTraceResult r = n.object.rayCast(point1, point2);
				if(!bestResult.hit || r.timeOfImpact < bestResult.timeOfImpact){
					bestResult = r;
				}
			} else {
				stack.push(n.child1);
				stack.push(n.child2);
			}
		}
		return bestResult;
	}
	
	public List<T> getObjectsIntersectingAABB(AxisAlignedBB box){
		List<T> list = new ArrayList<>();
		Stack<Node<T>> stack = new Stack<>();
		stack.push(root);
		while(!stack.isEmpty()){
			Node<T> n = stack.pop();
			if(!n.box.intersects(box))
				continue;
			if(n.isLeaf){
				list.add(n.object);
			} else {
				stack.push(n.child1);
				stack.push(n.child2);
			}
		}
		return list;
	}
	
	public void insert(T object){
		if(root == null){
			root = new Node<T>();
			root.setLeaf(object);
			root.updateAABB(margin);
		} else {
			Node<T> node = new Node<>();
			node.setLeaf(object);
			node.updateAABB(margin);
			insertNode(node);
		}
	}
	
	private Node<T> findBestPair(Node<T> node){
		//Branch and bound algorithm for finding the best pair for this node
		Node<T> bestSibling = root;
		float bestCost = root.box.union(node.box).area();
		Stack<Pair<Node<T>, Float>> stack = new Stack<>();
		float nCost = bestCost-root.box.area();
		stack.push(new Pair<>(root, nCost));
		while(!stack.isEmpty()){
			Pair<Node<T>, Float> p = stack.pop();
			Node<T> n = p.left;
			float parentCost = p.right;
			float uArea = n.box.union(node.box).area();
			float newParentDeltaCost = uArea-n.box.area() + parentCost;
			nCost = uArea + parentCost;
			if(nCost < bestCost){
				bestCost = nCost;
				bestSibling = n;
			}
			if(n.isLeaf){
			} else {
				if(node.box.area() + newParentDeltaCost <= bestCost){
					stack.push(new Pair<>(n.child1, newParentDeltaCost));
					stack.push(new Pair<>(n.child2, newParentDeltaCost));
				}
			}
		}
		return bestSibling;
	}
	
	private void insertNode(Node<T> node){
		//Find best sibling
		Node<T> pair = findBestPair(node);
		
		//Create new parent
		Node<T> oldParent = pair.parent;
		Node<T> newParent = new Node<>();
		newParent.setBranch(node, pair);
		
		if(oldParent == null){
			root = newParent;
		} else {
			if(oldParent.child1 == pair){
				oldParent.child1 = newParent;
			} else {
				oldParent.child2 = newParent;
			}
		}
		
		//Refit AABBs
		Node<T> parent = newParent;
		while(parent != null){
			parent.updateAABB(margin);
			rotate(parent);
			parent = parent.parent;
		}
	}
	
	//Self balancing
	private void rotate(Node<T> node){
		if(node.isLeaf || (node.child1.isLeaf && node.child2.isLeaf)){
			return;
		}
		//Check all 4 cases. Not sure if I'm actually supposed to do this, Catto didn't go into a lot of detail on the actual impl in his paper.
		float[] costs = new float[4];
		//Find the would be difference in area of each node's child if we swapped out either of its children with the node's other child.
		if(!node.child1.isLeaf){
			float c1Area = node.child1.box.area();
			costs[0] = c1Area-node.child1.child1.box.union(node.child2.box).area();
			costs[1] = c1Area-node.child1.child2.box.union(node.child2.box).area();
		} else {
			costs[0] = -Float.MAX_VALUE;
			costs[1] = -Float.MAX_VALUE;
		}
		if(!node.child2.isLeaf){
			float c2Area = node.child2.box.area();
			costs[2] = c2Area-node.child2.child1.box.union(node.child1.box).area();
			costs[3] = c2Area-node.child2.child2.box.union(node.child1.box).area();
		} else {
			costs[2] = -Float.MAX_VALUE;
			costs[3] = -Float.MAX_VALUE;
		}
		//Find the highest difference. Whichever reconfiguration results in less surface area gets used. If none of them have a lower surface area, return.
		float max = -Float.MAX_VALUE;
		int maxIdx = -1;
		for(int i = 0; i < 4; i ++){
			if(costs[i] > max){
				max = costs[i];
				maxIdx = i;
			}
		}
		if(max <= 0)
			return;
		//Depending on the best configuration, swap two nodes to get that configuration.
		switch(maxIdx){
		case 0:
			node.child2.parent = node.child1;
			Node<T> oldChild = node.child1.child2;
			node.child1.child2 = node.child2;
			node.child2 = oldChild;
			oldChild.parent = node;
			node.child1.updateAABB(margin);
			break;
		case 1:
			node.child2.parent = node.child1;
			oldChild = node.child1.child1;
			node.child1.child1 = node.child2;
			node.child2 = oldChild;
			oldChild.parent = node;
			node.child1.updateAABB(margin);
			break;
		case 2:
			node.child1.parent = node.child2;
			oldChild = node.child2.child2;
			node.child2.child2 = node.child1;
			node.child1 = oldChild;
			oldChild.parent = node;
			node.child2.updateAABB(margin);
			break;
		case 3:
			node.child1.parent = node.child2;
			oldChild = node.child2.child1;
			node.child2.child1 = node.child1;
			node.child1 = oldChild;
			oldChild.parent = node;
			node.child2.updateAABB(margin);
			break;
		}
	}
	
	/*private void insertNode(Node<T> node, Node<T> parent){
		if(parent.isLeaf){
			Node<T> newParent = new Node<>();
			newParent.parent = parent.parent;
			newParent.setBranch(node, parent);
			newParent.updateAABB(margin);
			parent = newParent;
		} else {
			float areaDiff1 = parent.child1.box.union(node.box).area() - parent.child1.box.area();
			float areaDiff2 = parent.child2.box.union(node.box).area() - parent.child2.box.area();
			
			if(areaDiff1 < areaDiff2){
				insertNode(node, parent.child1);
			} else {
				insertNode(node, parent.child2);
			}
		}
		parent.updateAABB(margin);
	}*/
	
	public void removeObject(T object){
		removeNode(object.removeUserData());
	}
	
	public void removeNode(Node<T> node){
		Node<T> parent = node.parent;
		if(parent != null){
			Node<T> sibling = node.getSibling();
			if(parent.parent != null){
				sibling.parent = parent.parent;
				if(parent == parent.parent.child1){
					parent.parent.child1 = sibling;
				} else {
					parent.parent.child2 = sibling;
				}
			} else {
				root = sibling;
				sibling.parent = null;
			}
		} else {
			root = null;
		}
	}
	
	public void updateTree(){
		if(root == null)
			return;
		if(root.isLeaf){
			root.updateAABB(margin);
		} else {
			invalids.clear();
			updateNode(root, invalids);
			for(Node<T> node : invalids){
				removeNode(node);
				node.updateAABB(margin);
				insertNode(node);
			}
		}
	}
	
	private void updateNode(Node<T> node, List<Node<T>> invalidNodes){
		if(node.isLeaf){
			if(!node.box.contains(node.object.getBoundingBox())){
				invalidNodes.add(node);
			}
		} else {
			updateNode(node.child1, invalidNodes);
			updateNode(node.child2, invalidNodes);
		}
	}
	
	public static class Node<T extends TreeObject<T>> {
		
		AxisAlignedBB box;
		T object;
		
		Node<T> parent = null;
		Node<T> child1 = null;
		Node<T> child2 = null;
		boolean isLeaf = true;
		
		public void setLeaf(T data){
			isLeaf = true;
			object = data;
			data.setUserData(this);
			
			child1 = null;
			child2 = null;
		}
		
		public void setBranch(Node<T> child1, Node<T> child2){
			isLeaf = false;
			
			this.child1 = child1;
			this.child2 = child2;
			child1.parent = this;
			child2.parent = this;
		}
		
		public void updateAABB(float margin){
			if(isLeaf){
				box = object.getBoundingBox().grow(margin);
			} else {
				box = child1.box.union(child2.box);
			}
		}
		
		public Node<T> getSibling(){
			return this == parent.child1 ? parent.child2 : parent.child1;
		}
	}
}
