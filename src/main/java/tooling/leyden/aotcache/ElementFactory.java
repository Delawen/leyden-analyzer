package tooling.leyden.aotcache;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import tooling.leyden.commands.CommonParameters;

@Dependent
public class ElementFactory {

	@Inject
	Information information;

	@Transactional
	public Element getOrCreate(String identifier, String type, String address) {
		System.out.println("getOrCreate(" + identifier + ", " + type + ")");
		return information
				.getElement(identifier, null, null, CommonParameters.ElementsToUse.both, type)
				.orElseGet(() -> getElement(identifier, type, address));
	}

	private Element getElement(String identifier, String type, String address) {
		Element e;

		switch (type) {
			case "Class" -> {
				e = new ClassObject(identifier);
			}
			case "Method" -> {
				e = new MethodObject(identifier);
				e = information.addExternalElement(e);
				String qualifiedName = identifier.substring(identifier.indexOf(" ") + 1);
				if (qualifiedName.contains("(")) {
					qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("("));
				}
				((MethodObject) e).setName(qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1));

				String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
				this.fillReturnClass((MethodObject) e, identifier);
				ClassObject classObject = (ClassObject) getOrCreate(className, "Class", null);
				classObject.addMethod((MethodObject) e);
				information.update(classObject);
				procesParameters((MethodObject) e, identifier);

				// Set identifier, for searching this element
				StringBuilder sb = new StringBuilder(((MethodObject) e).getReturnType() + " ");
				sb.append((((MethodObject) e).getClassObject() != null) ? ((MethodObject) e).getClassObject().getIdentifier() + "." + ((MethodObject) e).getName() : ((MethodObject) e).getName());
				sb.append("(");
				if (!((MethodObject) e).getParameters().isEmpty()) {
					sb.append(String.join(", ", ((MethodObject) e).getParameters()));
				}
				sb.append(")");
				e.setIdentifier(sb.toString());
			}
			case "ConstantPool" -> {
				e = new ConstantPoolObject(identifier);
			}
			case "KlassTrainingData", "MethodData", "MethodCounters", "MethodTrainingData",
				 "Symbol", "Object" -> {
				e = new ReferencingElement(identifier, type);
			}
			case "CompileTrainingData" -> {
				e = new CompileTrainingData(identifier);
			}
			default -> {
				e = new BasicObject(identifier);
				e.setType(type);
			}
		}

		//By default, all elements go here
		e = information.addExternalElement(e);
		//When we mark them as saved in the cache, we will move them from here

		if (address != null) {
			e.setAddress(address);
		}

		return e;
	}


	private void procesParameters(MethodObject method, final String identifier) {
		if (!identifier.contains("(") || !identifier.contains(")")) {
			return;
		}
		//Get parameter classes to add as references
//88 void java.util.Hashtable.reconstitutionPut(java.util.Hashtable$Entry[], java.lang.Object, java.lang.Object)
		String parameters[] = identifier.substring(identifier.indexOf("(") + 1, identifier.indexOf(")"))
				.split(", ");
		for (String parameter : parameters) {
			if (!parameter.isBlank()) {
				var classes = information.getElements(parameter, null, null, CommonParameters.ElementsToUse.cached, "Class")
						.map(ClassObject.class::cast)
						.toList();
				classes.forEach(method::addParameter);
				if (classes.isEmpty()) {
					method.addParameter(parameter);
					//Maybe it was an array:
					if (parameter.endsWith("[]")) {
						parameter = parameter.substring(0, parameter.length() - 2);
						information
								.getElements(parameter, null, null, CommonParameters.ElementsToUse.cached, "Class")
								.map(ClassObject.class::cast)
								.forEachOrdered(method::addReference);
					}
				}
			}
		}
	}

	private void fillReturnClass(MethodObject method, String identifier) {
		if (identifier.indexOf(" ") > 0) {
			method.setReturnType(identifier.substring(0, identifier.indexOf(" ")));
			information
					.getElements(method.getReturnType(), null, null, CommonParameters.ElementsToUse.cached, "Class")
					.map(ClassObject.class::cast)
					.forEach(method::addReference);
		}
	}

}
