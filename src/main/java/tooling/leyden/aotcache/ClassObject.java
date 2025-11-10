package tooling.leyden.aotcache;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * This element represents a class inside the AOT Cache.
 */
public class ClassObject extends ReferencingElement {

	private String name;
	private String packageName = "";
	private List<MethodObject> methods = new ArrayList<>();
	private String arrayPrefix = "";
	private Element klassTrainingData;
	private List<ReferencingElement> symbols = new ArrayList<>();
	private Boolean isClassLoader = false;

	public ClassObject(String identifier) {
		this.setName(identifier.substring(identifier.lastIndexOf(".") + 1));
		if (identifier.indexOf(".") > 0) {
			this.setPackageName(identifier.substring(0, identifier.lastIndexOf(".")));
		}

		if (this.getPackageName().equalsIgnoreCase("jdk.internal.loader")
				&& this.getName().startsWith("ClassLoaders")) {
			isClassLoader = true;
		}
	}

	public String getType() {
		return "Class";
	}

	public String getKey() {
		return arrayPrefix + getPackageName() + "." + getName();
	}

	public String getName() {
		return name;
	}

	public String getPackageName() {
		return packageName;
	}

	public List<MethodObject> getMethods() {
		return methods;
	}

	public Boolean isClassLoader() {
		return isClassLoader;
	}

	public List<ReferencingElement> getSymbols() {
		return symbols;
	}

	public void addSymbol(ReferencingElement symbol) {
		if (!this.getSymbols().contains(symbol)) {
			this.getSymbols().add(symbol);
			this.getSymbols().sort(Comparator.comparing(Element::getKey));
		}
		symbol.markAsReferenced(this);
	}

	public void setName(String name) {
		this.name = name;
	}

	public Element getKlassTrainingData() {
		return klassTrainingData;
	}

	public void setKlassTrainingData(Element klassTrainingData) {
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
			this.getMethods().sort(Comparator.comparing(Element::isTrained).thenComparing(Element::getKey));
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

	@Override
	public AttributedString getDescription(String leftPadding) {
		AttributedStringBuilder sb = new AttributedStringBuilder();
		sb.append(super.getDescription(leftPadding));
		sb.append(AttributedString.NEWLINE);
		sb.append(leftPadding + "This class is ");
		if (!Information.getMyself().cacheContains(this)) {
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
