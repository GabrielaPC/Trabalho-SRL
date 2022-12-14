package edu.wisc.cs.will.Boosting.Trees;

import edu.wisc.cs.will.Boosting.RDN.RegressionRDNExample;
import edu.wisc.cs.will.Boosting.RDN.WILLSetup;
import edu.wisc.cs.will.Boosting.Utils.BoostingUtils;
import edu.wisc.cs.will.DataSetUtils.Example;
import edu.wisc.cs.will.DataSetUtils.RegressionExample;
import edu.wisc.cs.will.FOPC.*;
import edu.wisc.cs.will.Utils.RegressionValueOrVector;
import edu.wisc.cs.will.Utils.Utils;
import edu.wisc.cs.will.Utils.condor.CondorFileWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClauseBasedTree  {

	WILLSetup setup;
	final ArrayList<Clause> regressionClauses;
	private final ArrayList<Clause> suppClauses;
	private boolean breakAfterFirstMatch;
	private final boolean addLeafId;

	ClauseBasedTree(WILLSetup setup) {
		regressionClauses = new ArrayList<>();
		suppClauses = new ArrayList<>();
		breakAfterFirstMatch = true;
		addLeafId = false;
		setSetup(setup);
		if (setup.learnClauses) {
			breakAfterFirstMatch = false;
		}
	}

	/*
	 * Computes the regression value for an example returned by this tree 
	 * @param ex - Example to be evaluated
	 * @return the regression value. Note: This is the value for just ONE tree.
	 */
	public RegressionValueOrVector getRegressionValue(Example ex) {
		RegressionValueOrVector sum = null;
		int id = 0;
		for (Clause clause : regressionClauses) {
			id++;
			RegressionValueOrVector wt = getRegressionClauseWt(clause, ex);
			if (wt != null) {
				if (sum != null) {
					sum.addValueOrVector(wt);
				} else {
					sum = wt;
				}
				if (breakAfterFirstMatch) {
					if (addLeafId && ex instanceof RegressionExample) {
						RegressionExample rex = (RegressionExample)ex;
						rex.leafId = rex.leafId + id + "_";
					}
					break;
				}
			}
		}
		// Possible for inference using mlns
		if (sum == null) {
			if (ex instanceof RegressionRDNExample) {
				RegressionRDNExample rex = (RegressionRDNExample)ex;
				if (!rex.isHasRegressionVector()) {
					sum = new RegressionValueOrVector(0.0);
				} else {
					int size = rex.getOutputVector().length;
					double[] sumVec = new double[size];
					Arrays.fill(sumVec, 0.0);
					sum = new RegressionValueOrVector(sumVec);
				}
			}
		}
		return sum;
	}

	/*
	 * 
	 * @param clause - the clause to evaluate against
	 * @param ex - the example to evaluate
	 * @return the regression value for this clause. Return a NaN if the example doesn't satisfy the clause.
	 */
	RegressionValueOrVector getRegressionClauseWt(Clause clause, Example ex) {
	
		// If the last argument is a constant
		Literal head = clause.posLiterals.get(0);
		Term    y    = head.getArgument(head.numberArgs() - 1);
		if (! ( y instanceof Variable)) {
			RegressionValueOrVector value = BoostingUtils.getRegressionValueOrVectorFromTerm(y);
			
			List<Literal> new_body = clause.getDefiniteClauseBody();
	
			// Remove cuts
			if (new_body.size() > 0 &&
					new_body.get(new_body.size()-1).equals(setup.getHandler().cutLiteral)) {
				new_body.remove(new_body.size() - 1);
			}
			
			// Remove the negation by failure literals
			for (int i = 0; i < new_body.size(); i++) {
				Literal lit = new_body.get(i);
				if(lit.predicateName.equals(setup.getHandler().standardPredicateNames.negationByFailure)) {
					new_body.remove(i);
					i--;
				}
			}
			// Remove the last argument of the head(regression value)
			Literal new_head_lit = head.copy(false);
			new_head_lit.removeArgument(y);
			
			List<Literal> new_head = new ArrayList<>();
			new_head.add(new_head_lit);
			
			Clause cl = new Clause(setup.getHandler(), new_head, new_body);
	
			if (setup.getOuterLooper().proveExample(cl, ex)) {
				return value;
			}
			
			return null;
		}
	
		// Variable so do a substitution. NEVER TESTED
		Utils.waitHere("Not tested the code for variable as regression value used in " + clause);
		BindingList theta  = setup.getInnerLooper().proveExampleAndReturnBindingList(clause, ex);
		if (theta == null) {
			ex.removeArgument(setup.getHandler().getExternalVariable("OutputVarTreeLeaf"));
			return null;
		}
		Term val = theta.lookup((Variable)y);
		if(val==null) {
			Utils.error("Unexpected: " + clause + ":" + ex + ":" + theta);
		}
		ex.removeArgument(setup.getHandler().getExternalVariable("OutputVarTreeLeaf"));
		return BoostingUtils.getRegressionValueOrVectorFromTerm(val);
	}

	public void reparseRegressionTrees() {
	
		for (int i = 0; i < regressionClauses.size(); i++) {
			Clause cl2=null;
			Clause cl = regressionClauses.get(i);
			if (cl!=null)  { 
				cl2 = setup.convertFactToClause(cl + ".");
			}
			regressionClauses.set(i, cl2);
		}
		for (Clause suppClause : suppClauses) {
			if (suppClause !=null)  { 
				Clause cl2 = setup.convertFactToClause(suppClause + ".");
				setup.getInnerLooper().getContext().getClausebase().assertBackgroundKnowledge(cl2);
			}
	
		}
	}

	public void saveToFile(String filename) throws IOException {
		Utils.ensureDirExists(filename);
		// Create a new file.
		BufferedWriter wr = new BufferedWriter(new CondorFileWriter(filename, false));
		saveToStream(wr);
		wr.close();
	}

	private void saveToStream(BufferedWriter wr) throws IOException {
		boolean oldAnon = setup.getHandler().underscoredAnonymousVariables;
		setup.getHandler().underscoredAnonymousVariables=false;
	
		wr.write(setup.getInnerLooper().getStringHandler().getStringToIndicateStringCaseSensitivity() + "\n");
		// Assume we don't change the variable indicator mid-run.
		wr.write(setup.getHandler().getStringToIndicateCurrentVariableNotation()+ "\n\n");
		for (Clause cl : regressionClauses) {
			wr.write(cl.toString() + ".");
			wr.newLine();
		}
		for (Clause cl : suppClauses) {
			wr.write(cl.toPrettyString() + ".");
			wr.newLine();
		}
		setup.getHandler().underscoredAnonymousVariables=oldAnon;
	}

	public void loadFromFile(String filename) {
		List<Sentence> sentences = setup.getInnerLooper().getParser().readFOPCfile(filename);
		help_load(sentences);
	}

	private void help_load(List<Sentence> sentences) {
		for (Sentence sentence : sentences) {
			List<Clause> clauses = sentence.convertToClausalForm();
			if (clauses.size() == 1) {
				Clause cl = clauses.get(0);
				if (cl.getDefiniteClauseHead().predicateName.name.contains("invented")) {
					addSupportingClause(cl);
					setup.getInnerLooper().getContext().getClausebase().assertBackgroundKnowledge(cl);
				} else {
					regressionClauses.add(cl);
				}
			} else {
				Utils.error("Not parsed as one clause. : " + clauses);
			}
		}
	}

	public void setSetup(WILLSetup setup) {
		this.setup = setup;
	}

	void addClause(Clause regressionClause) {
		regressionClauses.add(regressionClause);
	}

	void addSupportingClause(Clause regressionClause) {
		suppClauses.add(regressionClause);
	}

	public ArrayList<Clause> getRegressionClauses() {
		return regressionClauses;
	}

	void setBreakAfterFirstMatch(boolean breakAfterFirstMatch) {
		this.breakAfterFirstMatch = breakAfterFirstMatch;
	}

}