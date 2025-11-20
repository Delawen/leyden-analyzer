package tooling.leyden.aotcache;

import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import tooling.leyden.commands.CommonParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class represents a method inside the AOT Cache.
 */
@Entity
public class MethodObject extends ReferencingElement {

	@ManyToOne
	private ClassObject classObject;
	@OneToOne
	private BasicObject constMethod;
	@OneToOne
	private ReferencingElement methodData;
	@OneToOne
	private ReferencingElement methodCounters;
	@OneToOne
	private ReferencingElement methodTrainingData;
	@OneToMany(mappedBy = "method")
	private Set<CompileTrainingData> compileTrainingData;

	private String returnType;
	private List<String> parameters = new ArrayList<>();

	MethodObject(String identifier) {
		super(identifier, "Method");
	}

	public MethodObject() {

	}

	public ClassObject getClassObject() {
		return classObject;
	}

	public void setClassObject(ClassObject classObject) {
		this.classObject = classObject;
		addReference(classObject);
	}

	public BasicObject getConstMethod() {
		return constMethod;
	}

	public void setConstMethod(BasicObject constMethod) {
		this.constMethod = constMethod;
	}

	public ReferencingElement getMethodData() {
		return methodData;
	}

	public void setMethodData(ReferencingElement methodData) {
		this.methodData = methodData;
	}

	public ReferencingElement getMethodCounters() {
		return methodCounters;
	}

	public void setMethodCounters(ReferencingElement methodCounters) {
		this.methodCounters = methodCounters;
	}

	public ReferencingElement getMethodTrainingData() {
		return methodTrainingData;
	}

	public void setMethodTrainingData(ReferencingElement methodTrainingData) {
		this.methodTrainingData = methodTrainingData;
	}

	public Set<CompileTrainingData> getCompileTrainingData() {
		return compileTrainingData;
	}

	public void addCompileTrainingData(CompileTrainingData compileTrainingData) {
		this.compileTrainingData.add(compileTrainingData);
	}

	public void addParameter(ClassObject parameter) {
		this.parameters.add(parameter.getIdentifier());
		addReference(parameter);
	}

	//If Class is not found on the AOT Cache,
	//Maybe it is defined later?
	public void addParameter(String parameter) {
		this.parameters.add(parameter);
	}

	public List<String> getParameters() {
		return parameters;
	}

	public String getReturnType() {
		return returnType == null ? "void" : returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	@Override
	public boolean isTrained() {
		return this.getMethodTrainingData() != null
				|| (this.getCompileTrainingData() != null && !this.getCompileTrainingData().isEmpty());
	}

	@Override
	public boolean isTraineable() {
		return true;
	}

	@Override
	public AttributedString getDescription(String leftPadding) {
		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.append(super.getDescription(leftPadding));
		sb.append(AttributedString.NEWLINE);
		sb.append(leftPadding + "Belongs to the class ");
		sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
		sb.append(getClassObject().getIdentifier());
		sb.style(AttributedStyle.DEFAULT);

		if (this.methodCounters != null) {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "It has a ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append("MethodCounters");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" associated to it, which means it was called at least once during training run.");
			sb.style(AttributedStyle.DEFAULT);
		} else {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding);
			sb.style(AttributedStyle.DEFAULT.bold());
			sb.append("This method doesn't seem to have been called during training run.");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "If you think this method should be part of the training, make sure your " +
					"training run use it repeatedly as it would on a production run.");
		}

		if (!this.compileTrainingData.isEmpty()) {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "It has ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append("CompileTrainingData");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" associated to it on level:");
			sb.style(AttributedStyle.DEFAULT.bold());
			for (CompileTrainingData ctd : this.compileTrainingData) {
				sb.append(" " + ctd.getLevel());
			}
			sb.style(AttributedStyle.DEFAULT);
		} else {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "It has no ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
			sb.append("CompileTrainingData");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" associated to it.");
		}

		if (this.methodData != null) {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "It has a ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append("MethodData");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" associated to it.");
		}

		sb.append(AttributedString.NEWLINE);
		if (this.methodTrainingData != null) {
			sb.append(leftPadding + "It has a ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
		} else {
			sb.append(leftPadding + "It has no ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
		}
		sb.append("MethodTrainingData");
		sb.style(AttributedStyle.DEFAULT);
		sb.append(" associated to it.");
		return sb.toAttributedString();
	}

	public void setCompileTrainingData(Set<CompileTrainingData> compileTrainingData) {
		this.compileTrainingData = compileTrainingData;
	}
}
