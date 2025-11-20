package tooling.leyden.aotcache;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class CompileTrainingData extends BasicObject {
	private Integer level;

	@ManyToOne
	private MethodObject method;

	public CompileTrainingData(String name) {
		super(name);
		this.setType("CompileTrainingData");
	}

	public CompileTrainingData() {

	}

	public MethodObject getMethod() {
		return method;
	}

	public void setMethod(MethodObject method) {
		this.method = method;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}
}
