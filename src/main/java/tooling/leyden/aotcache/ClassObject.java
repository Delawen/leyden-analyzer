package tooling.leyden.aotcache;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.transaction.Transactional;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This element represents a class inside the AOT Cache.
 */
@Entity
public class ClassObject extends ReferencingElement {

	private String name;
	private String packageName = "";
	private String arrayPrefix = "";
	private Boolean isClassLoader = false;
	@OneToOne
	private ReferencingElement klassTrainingData;
	@OneToMany(mappedBy = "classObject")
	private Set<MethodObject> methods = new HashSet<>();
	@OneToMany
	private Set<ReferencingElement> symbols = new HashSet<>();
	@OneToOne
	private ConstantPoolObject constantPoolObject;

	ClassObject(String identifier) {
		super(identifier, "Class");
		this.setName(identifier.substring(identifier.lastIndexOf(".") + 1));
		if (identifier.indexOf(".") > 0) {
			this.setPackageName(identifier.substring(0, identifier.lastIndexOf(".")));
		}

		if (this.getPackageName().equalsIgnoreCase("jdk.internal.loader")
				&& this.getName().startsWith("ClassLoaders")) {
			isClassLoader = true;
		}

		setIdentifier(arrayPrefix + getPackageName() + "." + getName());
	}

	public ClassObject() {

	}

	public String getType() {
		return "Class";
	}

	public String getName() {
		return name;
	}

	public String getPackageName() {
		return packageName;
	}

	public Set<MethodObject> getMethods() {
		return methods;
	}

	public Boolean isClassLoader() {
		return isClassLoader;
	}

	public Set<ReferencingElement> getSymbols() {
		return symbols;
	}

	@Transactional
	public void addSymbol(ReferencingElement symbol) {
		this.getSymbols().add(symbol);
		symbol.markAsReferenced(this);
	}

	public void setName(String name) {
		this.name = name;
	}

	public Element getKlassTrainingData() {
		return klassTrainingData;
	}

	public void setKlassTrainingData(ReferencingElement klassTrainingData) {
		this.klassTrainingData = klassTrainingData;
	}

	public void setPackageName(String packageName) {
		while (packageName.startsWith("[")) {
			if (packageName.startsWith("[L")) {
				arrayPrefix += "[L";
				packageName = packageName.substring(2);
			} else {
				arrayPrefix += "[";
				packageName = packageName.substring(1);
			}
		}
		this.packageName = packageName;
	}

	public void addMethod(MethodObject method) {
		if (!this.methods.contains(method)) {
			this.methods.add(method);
			method.setClassObject(this);
		}
		method.markAsReferenced(this);
	}

	public Boolean isArray() {
		return !this.arrayPrefix.isBlank();
	}

	@Override
	public boolean isTrained() {
		return this.getKlassTrainingData() != null;
	}

	@Override
	public boolean isTraineable() {
		return true;
	}

	public ConstantPoolObject getConstantPoolObject() {
		return constantPoolObject;
	}

	public void setConstantPoolObject(ConstantPoolObject constantPoolObject) {
		this.constantPoolObject = constantPoolObject;
	}

	@Override
	public AttributedString getDescription(String leftPadding) {
		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.append(super.getDescription(leftPadding));
		sb.append(AttributedString.NEWLINE);
		sb.append(leftPadding + "This class is ");
		if (this.isCached()) {
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
			sb.append("NOT ");
			sb.style(AttributedStyle.DEFAULT);
		}
		sb.append("included in the AOT cache.");

		if (isClassLoader()) {
			sb.append(AttributedString.NEWLINE);
			sb.style(AttributedStyle.DEFAULT.bold());
			sb.append(leftPadding + "This class is a class loader.");
			sb.style(AttributedStyle.DEFAULT);
		}

		int trained = 0;
		int run = 0;
		if (!this.getMethods().isEmpty()) {
			sb.append(AttributedString.NEWLINE);
			sb.append(leftPadding + "This class has ");
			sb.style(AttributedStyle.DEFAULT.bold());
			sb.append(Integer.toString(this.getMethods().size()));
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.YELLOW));
			sb.append(" Methods");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(", of which");

			for (MethodObject method : this.getMethods()) {
				if (method.getMethodCounters() != null) {
					run++;
				}
				if (method.isTrained()) {
					trained++;
				}
			}
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append(" " + run);
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" have been run and");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append(" " + trained);
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" have been trained.");
		}
		sb.append(AttributedString.NEWLINE);

		if (this.klassTrainingData != null) {
			sb.append(leftPadding + "It has a ");
			sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
			sb.append("KlassTrainingData");
			sb.style(AttributedStyle.DEFAULT);
			sb.append(" associated to it.");
		} else {
			sb.style(AttributedStyle.DEFAULT.bold());
			sb.append(leftPadding + "This class doesn't seem to have training data. ");
			sb.style(AttributedStyle.DEFAULT);
			if (trained == 0) {
				sb.append("If you think this class and its methods should be part of the training, make sure your " +
						"training run use them.");
			}
		}


		return sb.toAttributedString();
	}

}
