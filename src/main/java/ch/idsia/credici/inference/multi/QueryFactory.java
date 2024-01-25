package ch.idsia.credici.inference.multi;

import java.util.ArrayList;
import java.util.List;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * A class representing a query on a world
 * Can be concatenated with other worlds and generate a multiple world counterfactual query.
 */
public class QueryFactory {

	class QueryPart {
		protected TIntIntMap doing;
		protected TIntIntMap observing;
		protected TIntSet conditioning;		
		boolean bar = false;
		
		private QueryPart() {
			doing = new TIntIntHashMap();
			observing = new TIntIntHashMap();
			conditioning = new TIntHashSet();
		}
		
		public TIntIntMap getDoing() {
			return doing;
		}
		
		public TIntIntMap getObserving() {
			return observing;
		}
		
		public TIntSet getConditioning() {
			return conditioning;
		}
	}

	protected List<QueryPart> worlds;
	protected QueryPart current;
	

	private QueryFactory() {
		this.worlds = new ArrayList<QueryFactory.QueryPart>();
	
	}

	public QueryFactory doing(int variable, int state) {
		if (current.doing.containsKey(variable) || current.observing.containsKey(variable))
			throw new IllegalStateException("Multiple usage without counterfactual");

		current.doing.put(variable, state);
		if (current.bar)
			current.conditioning.add(variable);

		return this;
	}

	public QueryFactory observing(int variable, int state) {
		if (current.doing.containsKey(variable) || current.observing.containsKey(variable))
			throw new IllegalStateException("Multiple usage without counterfactual");

		current.observing.put(variable, state);
		if (current.bar)
			current.conditioning.add(variable);

		return this;
	}

	public QueryFactory given() {
		current.bar = true;
		return this;
	}

	public QueryFactory counterfactual() {
		current = new QueryPart();
		worlds.add(current);
		return this;
	}
	
	private QueryFactory start() {
		current = new QueryPart();
		worlds.add(current);
		return this;
	}
	
	public List<QueryPart> build() {
		return worlds;
	}
	
	public static QueryFactory builder() {
		QueryFactory builder = new QueryFactory();
		return builder.start();
	}

	
	public static void main(String[] args) {
		QueryFactory.builder()
			.observing(1,2).given().observing(2,2)
			.counterfactual()
			.observing(1,1).given().observing(2,1)
			.build();
	}
}
