package edu.wisc.cs.will.FOPC;

import edu.wisc.cs.will.ILP.SingleClauseNode;
import edu.wisc.cs.will.Utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TreeStructuredTheoryInteriorNode extends TreeStructuredTheoryNode {

	// Note: the fullNodeTest is the complete path to the root and the nodeTest is the local piece (null means 'true').  For simplicity the local piece
	//       also is a clause rather than a List<Literal>.  Do the order this code was generated, initially there was no fullNodeTest and so some methods could be simplified now that there is. 
	private Clause                   fullNodeTest;
	private Clause                   nodeTest;
	private SingleClauseNode         searchNodeThatLearnedTheClause; // This is the search node that produced this tree-structured node (i.e., learned the node test).
	private TreeStructuredTheoryNode treeForTrue;
	private TreeStructuredTheoryNode treeForFalse;

	// 8/8/11 : TVK set this to true. RDN-Boost code will take care of dropping the false nodes whereas the MLN code needs the false tests.
	// TODO(@hayesall): This should have no knowledge of boosting, refactor.

	private double				       regressionValueIfLeaf= 0;    // Since we reset the examples while trying to expand a node, this value is lost. Rather than re-computing it, we cache it here.
	private List<Boolean>			   treePath = new ArrayList<>();
	
	public TreeStructuredTheoryInteriorNode(double weightedCountOfPositiveExamples, double weightedCountOfNegativeExamples, Clause learnedClause, Clause localClause, SingleClauseNode searchNodeThatLearnedTheClause) {
		super();
		// The two node tests will probably be set in ILPouterLoop.
		this.fullNodeTest = learnedClause;
		this.nodeTest     = localClause;
		this.weightedCountOfPositiveExamples = weightedCountOfPositiveExamples;
		this.weightedCountOfNegativeExamples = weightedCountOfNegativeExamples;
		this.searchNodeThatLearnedTheClause  = searchNodeThatLearnedTheClause;
	}		
	private TreeStructuredTheoryInteriorNode(double weightedCountOfPositiveExamples, double weightedCountOfNegativeExamples, Clause learnedClause, Clause localClause, SingleClauseNode searchNodeThatLearnedTheClause, TreeStructuredTheoryNode treeForTrue, TreeStructuredTheoryNode treeForFalse) {
		this(weightedCountOfPositiveExamples, weightedCountOfNegativeExamples, learnedClause, localClause, searchNodeThatLearnedTheClause);
		this.treeForTrue  = treeForTrue;
		this.treeForFalse = treeForFalse;
	}	

	// Climb the ancestors of seeker to find the last parent of a TRUE branch.
	public TreeStructuredTheoryInteriorNode getLastParentOnTrueBranch(TreeStructuredTheoryInteriorNode seeker) {
		if (treeForTrue == seeker) { return this; }
		TreeStructuredTheoryInteriorNode parent = getParent();
		if (parent == null) { return null; }
		// Fixed this bug which matched the leaf node (seeker) in the right and left child
		// of all the ancestors. This would obviously never return any match. (TVK 17th April 2012) 
		// return parent.getLastParentOnTrueBranch(seeker);
		// Instead search for the current node to match the parent's right and left child. This bug
		// probably made our code less accurate since we missed some possible rules.  
		return parent.getLastParentOnTrueBranch(this);
	}

	// Need to change a child node.
	public void changeChild(TreeStructuredTheoryNode oldNode, TreeStructuredTheoryNode newNode) {
		if      (treeForTrue  == oldNode) { treeForTrue  = newNode; }
		else if (treeForFalse == oldNode) { treeForFalse = newNode; } // ASSUMES THAT oldNode DOES NOT EQUAL *BOTH* CHILDREN!
		else {
			Utils.error("Could not find\n'" + oldNode + "'\n in\n'" + this + "'.");
		}		
	}

	public Clause getNodeTest() {
		return nodeTest;
	}

	public void setNodeTestFromFullNodeTest(Clause fullNodeTest) {
		this.fullNodeTest = fullNodeTest;
		this.nodeTest     = (fullNodeTest == null ? null : (searchNodeThatLearnedTheClause == null ? fullNodeTest : searchNodeThatLearnedTheClause.getLocallyAddedClause()));
	}

	public SingleClauseNode getSearchNodeThatLearnedTheClause() {
		return searchNodeThatLearnedTheClause;
	}

	public void setSearchNodeThatLearnedTheClause(SingleClauseNode searchNodeThatLearnedTheClause) {
		this.searchNodeThatLearnedTheClause = searchNodeThatLearnedTheClause;
	}

	public void setTreeForTrue(TreeStructuredTheoryNode treeForTrue) {
		this.treeForTrue = treeForTrue;
		treeForTrue.setLevel(getLevel() + 1);
	}

	public void setTreeForFalse(TreeStructuredTheoryNode treeForFalse) {
		this.treeForFalse = treeForFalse;
		treeForFalse.setLevel(getLevel() + 1);
	}
	
	private String createThisManySpaces(int depth) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < depth; i++) { result.append("| "); }
		return result.toString();
	}
	
	@Override
	protected String toPrettyString(String newLineStarter, int precedenceOfCaller, BindingList bindingList) {
		return printRelationalTree(newLineStarter, precedenceOfCaller, 0, bindingList);
	}
	@Override
	public String toString(int precedenceOfCaller, BindingList bindingList) {
		return toPrettyString("", precedenceOfCaller, bindingList);
	}
	
	public void addToPath(boolean branch) {
		treePath.add(branch);
	}
	
	public List<Boolean> returnBoolPath() {
		return treePath;
	}
	public void setBoolPath(List<Boolean> path) {
		treePath = new ArrayList<>(path);
	}
	private String writeClauseBody(BindingList bl) {
		StringBuilder result = new StringBuilder();
		boolean firstTime = true;
		
		Collection<Literal> negLits = (nodeTest == null ? null : nodeTest.negLiterals);
		if (Utils.getSizeSafely(negLits) < 1) { return "true"; }

		assert negLits != null;
		for (Literal lit : negLits) {
			if (firstTime) { firstTime = false; } else { result.append(", "); }
			result.append(lit.toString(bl));
		}
		return result.toString();
	}

	@Override
	public String printRelationalTree(String newLineStarter, int precedenceOfCaller, int depth, BindingList bindingList) {
		if (depth > 10) { return " ... <tree too deep>"; }
		BindingList blCopy = new BindingList(bindingList.theta);
		return "if ( " + writeClauseBody(blCopy) + " )\n"
				+ newLineStarter + createThisManySpaces(depth) + "then " + ( treeForTrue == null ? "???" :  treeForTrue.printRelationalTree(newLineStarter, precedenceOfCaller, depth + 1, blCopy)) + "\n"
				+ newLineStarter + createThisManySpaces(depth) + "else " + (treeForFalse == null ? "???" : treeForFalse.printRelationalTree(newLineStarter, precedenceOfCaller, depth + 1, blCopy));
	}

	@Override
	public List<Clause> collectPathsToRoots(TreeStructuredTheory treeTheory) {
		List<Clause> clausesTrue  = ( treeForTrue  == null ? null :  treeForTrue.collectPathsToRoots(treeTheory));
		List<Clause> clausesFalse = (treeForFalse  == null ? null : treeForFalse.collectPathsToRoots(treeTheory));
		
		List<Literal> newLits = (nodeTest == null ? null : nodeTest.negLiterals);
		
		if (Utils.getSizeSafely(newLits) < 1) {
			Utils.waitHere("Have an empty nodeTest! " + this);
			if (clausesTrue  == null) { return clausesFalse; }
			if (clausesFalse == null) { return clausesTrue;  }
			clausesTrue.addAll(clausesFalse);
			return clausesTrue;
		}
		
		int           numbNewLits = Utils.getSizeSafely(newLits);

		assert newLits != null;
		Literal           newHead = newLits.get(0);
		// Not needed due to semantics of Prolog and our use of CUTs.

		List<Clause> results = new ArrayList<>(Utils.getSizeSafely(clausesTrue));
		if (clausesTrue != null) for (Clause c : clausesTrue) {
			Clause cCopy = c.copy(false);
			if (numbNewLits < 2) {
				cCopy.addNegLiteralToFront(newHead);
			} else {
				for (int i = numbNewLits - 1; i >= 0; i--) { cCopy.addNegLiteralToFront(newLits.get(i)); } // Need to maintain left-to-right order.
			}
			results.add(cCopy);
		}
		if (clausesFalse != null) {
			// If collectTestForFalseNodes is true, create invented supporting predicates ?
			for (Clause c : clausesFalse) {
				Clause cCopy = c.copy(false);
				results.add(cCopy);
			}
		}
		return results;
	}

	@Override
	public TreeStructuredTheoryInteriorNode applyTheta(Map<Variable, Term> bindings) {
		return new TreeStructuredTheoryInteriorNode(weightedCountOfPositiveExamples, weightedCountOfNegativeExamples,
													(fullNodeTest == null ? null : fullNodeTest.applyTheta(bindings)),
													(nodeTest     == null ? null : nodeTest.applyTheta(bindings)),
													searchNodeThatLearnedTheClause, // NOTE: do to renaming this node's variables will no longer match the variables in this node.
													(treeForTrue  == null ? null : treeForTrue.applyTheta(bindings)),
													(treeForFalse == null ? null : treeForFalse.applyTheta(bindings)));
	}

    @Override
	public Collection<Variable> collectFreeVariables(Collection<Variable> boundVariables) {
		Collection<Variable> freeA = (nodeTest     == null ? null : nodeTest.collectFreeVariables(    boundVariables));
		Collection<Variable> freeB = (treeForTrue  == null ? null : treeForTrue.collectFreeVariables( boundVariables));
		Collection<Variable> freeC = (treeForFalse == null ? null : treeForFalse.collectFreeVariables(boundVariables));
						
		return Variable.combineSetsOfVariables(Variable.combineSetsOfVariables(freeA, freeB), freeC);
	}
	@Override
	public String writeDotFormat() {
		// Set by parent
		if (this.nodeNumber == 0) {
			nodeNumber = counter++;
		}
		String result = "";
		if (nodeTest != null) { 
			result += nodeNumber + "[label = \"" + nodeTest.getDefiniteClauseBody() + "\"];\n";
		} else {
			result += nodeNumber + "[label = \"" + "null" + "\"];\n";
		}
		if (treeForTrue != null) {
			treeForTrue.nodeNumber = counter++;
			result = result + nodeNumber + " -> " + treeForTrue.nodeNumber + "[label=\"True\"];\n";
			
		}
		
		if (treeForFalse != null) {
			treeForFalse.nodeNumber = counter++;
			result = result + nodeNumber + " -> " + treeForFalse.nodeNumber + "[label=\"False\"];\n";
		}
		if (treeForTrue != null) {
			result = result + treeForTrue.writeDotFormat();
		}
		if (treeForFalse != null) {
			result = result + treeForFalse.writeDotFormat();
		}
		return result;
	}

	@Override
	public int countVarOccurrencesInFOPC(Variable v) {
		return (nodeTest == null ? 0 : nodeTest.countVarOccurrencesInFOPC(v));
	}	

	public double getRegressionValueIfLeaf() {
		return regressionValueIfLeaf;
	}
	public void setRegressionValueIfLeaf(double regressionValueIfLeaf) {
		this.regressionValueIfLeaf = regressionValueIfLeaf;
	}

}
