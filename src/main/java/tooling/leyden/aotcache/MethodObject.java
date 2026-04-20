package tooling.leyden.aotcache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * This class represents a method inside the AOT Cache.
 */
public class MethodObject extends ReferencingElement {

    private ClassObject classObject;
    private BasicObject constMethod;
    private Element methodData;
    private Element methodCounters;
    private Element methodTrainingData;
    private Map<Integer, Element> compileTrainingData = new HashMap<>();

    private String returnType;
    private List<String> parameters = new ArrayList<>();

    MethodObject(String identifier) {
        super(identifier, "Method");
        String qualifiedName = identifier.substring(identifier.indexOf(" ") + 1);
        if (qualifiedName.contains("(")) {
            qualifiedName = qualifiedName.substring(0, qualifiedName.indexOf("("));
        }
        this.setName(qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1));
        String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        this.fillReturnClass(identifier);
        this.fillClass(className);
        this.procesParameters(identifier);
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

    public Element getMethodData() {
        return methodData;
    }

    public void setMethodData(Element methodData) {
        this.methodData = methodData;
    }

    public Element getMethodCounters() {
        return methodCounters;
    }

    public void setMethodCounters(Element methodCounters) {
        this.methodCounters = methodCounters;
    }

    public Element getMethodTrainingData() {
        return methodTrainingData;
    }

    public void setMethodTrainingData(Element methodTrainingData) {
        this.methodTrainingData = methodTrainingData;
    }

    public Map<Integer, Element> getCompileTrainingData() {
        return compileTrainingData;
    }

    public void addCompileTrainingData(Integer level, Element compileTrainingData) {
        this.compileTrainingData.put(level, compileTrainingData);
    }

    public void addParameter(Element parameter) {
        this.parameters.add(parameter.getKey());
        addReference(parameter);
    }

    //If Class is not found on the AOT Cache,
    //Maybe it is defined later?
    public void addParameter(String parameter) {
        this.parameters.add(parameter);
    }

    public String getReturnType() {
        return returnType == null ? "void" : returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    @Override
    public String getKey() {
        StringBuilder sb = new StringBuilder(getReturnType() + " ");
        sb.append((getClassObject() != null) ? getClassObject().getKey() + "." + getName() : getName());
        sb.append("(");
        if (!parameters.isEmpty()) {
            sb.append(String.join(", ", parameters));
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean isTrained() {
        return !this.getCompileTrainingData().isEmpty();
    }

    @Override
    public boolean isTraineable() {
        return true;
    }

    @Override
    public AttributedString getDescription(String leftPadding, Boolean verbose, Boolean tips) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append(super.getDescription(leftPadding, verbose, tips));
        sb.append(AttributedString.NEWLINE);
        if (verbose) {
            if (this.getName().equals("<init>()")) {
                sb.append(leftPadding).append("  ℹ\uFE0F  This method is ")
                        .append(this.parameters.isEmpty() ? "the default" : "an")
                        .append(" instance initialization method (constructor).");
                sb.append(AttributedString.NEWLINE);
            } else if (getName().equals("<clinit>")) {
                sb.append(leftPadding).append("  ℹ\uFE0F  This method is a static initialization method.");
                sb.append(AttributedString.NEWLINE);
            }
        }
        sb.append(leftPadding).append("Training Information: ");
        sb.append(AttributedString.NEWLINE);
        leftPadding = "  " + leftPadding;

        if (this.getMethodCounters() != null) {
            sb.append(leftPadding).append("It has a ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
            sb.append("MethodCounters");
            sb.style(AttributedStyle.DEFAULT);
            sb.append(" associated to it.");
            if (verbose) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT));
                sb.append(leftPadding).append("  ℹ\uFE0F  This means it was called significantly during training run.");
            }
            sb.style(AttributedStyle.DEFAULT);
        } else {
            sb.append(leftPadding).append("It has no ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
            sb.append("MethodCounters");
            sb.style(AttributedStyle.DEFAULT);
            sb.append(" associated to it.");
            if (verbose) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT));
                sb.append(leftPadding).append("  ℹ\uFE0F  This method doesn't seem to have been called significantly during training run.");
            }
            sb.style(AttributedStyle.DEFAULT);
        }
        sb.append(AttributedString.NEWLINE);

        if (this.getMethodData() != null) {
            sb.append(leftPadding).append("It has a ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
            sb.append("MethodData");
            sb.style(AttributedStyle.DEFAULT);
            sb.append(" associated to it.");
            if (verbose) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT));
                sb.append(leftPadding).append("  ℹ\uFE0F  This means it is highly profiled.");
            }
        } else {
            sb.append(leftPadding).append("It has no ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
            sb.append("MethodData");
            sb.style(AttributedStyle.DEFAULT);
            sb.append(" associated to it.");
            if (verbose) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT));
                sb.append(leftPadding).append("  ℹ\uFE0F  This means it may be profiled, but not ready to be compiled on a high level.");
            }
            if (tips) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
                sb.append(leftPadding).append("  \uD83D\uDCA1  If this is a key method in your app, you should have this asset.");
            }
            sb.style(AttributedStyle.DEFAULT);
        }
        sb.append(AttributedString.NEWLINE);

        if (!this.getCompileTrainingData().isEmpty()) {
            sb.append(leftPadding).append("It has ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
            sb.append("CompileTrainingData");
            sb.style(AttributedStyle.DEFAULT);
            sb.append(" associated to it on level:");
            sb.style(AttributedStyle.DEFAULT.bold());
            for (Integer level : this.compileTrainingData.keySet()) {
                sb.append(" ").append(String.valueOf(level));
            }
            if (verbose) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT));
                sb.append(leftPadding).append("  ℹ\uFE0F  Higher compilation levels mean a more optimized compilation.");
            }
            if (tips) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
                sb.append(leftPadding).append("  \uD83D\uDCA1  Key methods should aim for compilation 3 or above.");
            }
            sb.style(AttributedStyle.DEFAULT);
        } else {
            sb.append(leftPadding).append("It has no ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
            sb.append("CompileTrainingData");
            sb.style(AttributedStyle.DEFAULT);
            sb.append(" associated to it.");
            if (verbose) {
                sb.append(AttributedString.NEWLINE);
                sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT));
                sb.append(leftPadding).append("  ℹ\uFE0F  This method was not considered for optimization during training run.");
            }
            sb.style(AttributedStyle.DEFAULT);
        }
        sb.append(AttributedString.NEWLINE);

        if (this.methodTrainingData != null) {
            sb.append(leftPadding).append("It has a ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
        } else {
            sb.append(leftPadding).append("It has no ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.RED));
        }
        sb.append("MethodTrainingData");
        sb.style(AttributedStyle.DEFAULT);
        sb.append(" associated to it.");

        if (tips) {
            sb.append(AttributedString.NEWLINE);
            sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
            sb.append(leftPadding).append("  \uD83D\uDCA1  If you think the training for this method is not good enough, make sure your ").append("training run use it more, as it would on a long production run.");
        }

        return sb.toAttributedString();
    }

    private void procesParameters(final String identifier) {
        if (!identifier.contains("(") || !identifier.contains(")")) {
            return;
        }
        //Get parameter classes to add as references
        //88 void java.util.Hashtable.reconstitutionPut(java.util.Hashtable$Entry[], java.lang.Object, java.lang.Object)
        String[] parameters = identifier.substring(identifier.indexOf("(") + 1, identifier.indexOf(")"))
                .split(", ");
        for (String parameter : parameters) {
            if (!parameter.isBlank()) {
                var classes = Information.getMyself().getElements(parameter, null, null, true, true, "Class").toList();
                classes.forEach(this::addParameter);
                if (classes.isEmpty()) {
                    this.addParameter(parameter);
                    //Maybe it was an array:
                    if (parameter.endsWith("[]")) {
                        parameter = parameter.substring(0, parameter.length() - 2);
                        Information.getMyself()
                                .getElements(parameter, null, null, true, true, "Class")
                                .forEachOrdered(this::addReference);
                    }
                }
            }
        }
    }

    private void fillClass(String className) {
        classObject = (ClassObject) ElementFactory.getOrCreate(className, "Class", null);
        classObject.addMethod(this);
    }

    private void fillReturnClass(String identifier) {
        if (identifier.indexOf(" ") > 0) {
            this.setReturnType(identifier.substring(0, identifier.indexOf(" ")));
            Information.getMyself()
                    .getElements(this.getReturnType(), null, null, true, true, "Class")
                    .forEach(this::addReference);
        }
    }
}
