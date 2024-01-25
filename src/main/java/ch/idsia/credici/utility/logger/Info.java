package ch.idsia.credici.utility.logger;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import ch.idsia.credici.utility.table.DoubleTable;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.GraphicalModel;

public class Info {
	private GraphicalModel<BayesianFactor> model;
	private DoubleTable data;
	private String modelName;
	private String title;
	private Function<Integer, String> nodeName;
	private Map<Integer, Set<Integer>> highlight;
	
	public Info model(GraphicalModel<BayesianFactor> model) {
		this.model = model;
		return this;
	}

	public GraphicalModel<BayesianFactor> getModel() {
		return model;
	}
	
	public DoubleTable getData() {
		return data;
	}

	public Info data(DoubleTable data) {
		this.data = data;
		return this;
	}

	public String getModelName() {
		return modelName;
	}

	public Info modelName(String modelName) {
		this.modelName = modelName;
		return this;
	}

	public String getTitle() {
		return title;
	}

	public Info title(String title) {
		this.title = title;
		return this;
	}

	public Function<Integer, String> getNodeName() {
		return nodeName;
	}

	public Info nodeName(Function<Integer, String> nodeName) {
		this.nodeName = nodeName;
		return this;
	}

	public Map<Integer, Set<Integer>> getHighlight() {
		return highlight;
	}

	public Info highlight(Map<Integer, Set<Integer>> highlight) {
		this.highlight = highlight;
		return this;
	}

	public Info(GraphicalModel<BayesianFactor> model, DoubleTable data, String modelName, String title,
			Function<Integer, String> nodeName, Map<Integer, Set<Integer>> highlight) {
		super();
		this.model = model;
		this.data = data;
		this.modelName = modelName;
		this.title = title;
		this.nodeName = nodeName;
		this.highlight = highlight;
	}

	public Info() {
	}	
}
