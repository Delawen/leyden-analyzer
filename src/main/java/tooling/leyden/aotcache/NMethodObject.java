package tooling.leyden.aotcache;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import tooling.leyden.commands.CommonParameters;
import tooling.leyden.commands.logparser.AOTMapParser;

import java.util.*;

/**
 * This class represents a native compiled method inside the AOT Cache.
 */
public class NMethodObject extends CodeObject {

    private MethodObject method;
    private Integer compilationLevel;
    private CodeObject adapter;

    public NMethodObject(String identifier) {
        super(identifier, "NMethod");
    }

    public MethodObject getMethod() {
        return method;
    }

    public void setMethod(String identifier) {
        // translate method signature

        // primitives like  [I convert to int[]
        // int[] java.math.BigInteger.subtract(int[], int[])
        // java.math.BigInteger.subtract([I[I)[I

        String[] parameters = identifier.substring(identifier.indexOf("(") + 1, identifier.lastIndexOf(")")).split(";");
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = translateSymbols(parameters[i]);
        }
        String returnType = translateSymbols(identifier.substring(identifier.lastIndexOf(")") + 1));

        String signature = identifier.substring(0, identifier.indexOf("("));

        // move return type to the front
        // java.math.BigInteger.subtract(Ljava/math/BigInteger;)Ljava/math/BigInteger;
        // java.math.BigInteger java.math.BigInteger.subtract(java.math.BigInteger)
        String methodIdentifier = returnType + " " + signature + "(" + String.join(", ", parameters) + ")";
        this.method = (MethodObject) ElementFactory.getOrCreate(methodIdentifier, "Method", null);
        this.addReference(this.getMethod());

        //Search for the adapter, which should come first on the code cache
        CommonParameters cparameters = new CommonParameters();
        cparameters.setTypes(new String[]{"Adapter"});
        cparameters.setNameLike("(.)* " + this.method.getAdapterSignature());
        Information.getMyself().getElements(cparameters).findAny()
                .ifPresent(a -> this.adapter = (CodeObject) a);
        if (this.adapter == null) {
            Information.getMyself().addWarning(this, "This nmethod does not have an adapter.", WarningType.CacheCreation);
        } else {
            addReference(this.adapter);
        }
    }

    private String translateSymbols(String symbol) {
        //Take care of arrays
        List<String> symbols = new ArrayList<>(Arrays.asList(symbol.split("\\[")));
        if (symbols.size() > 1) {
            symbols.removeFirst();
            symbols = symbols.stream().map(s -> translateSymbol(s) + "[]").toList();
        } else {
            symbols = symbols.stream().map(s -> translateSymbol(s)).toList();
        }

        return String.join(", ", symbols);
    }

    private String translateSymbol(String symbol) {
        symbol = symbol.replaceAll("/", ".");

        if (symbol.startsWith("L") && symbol.endsWith(";")) {
            symbol = symbol.substring(1, symbol.length() - 1);
        } else if (symbol.startsWith("L")) {
            symbol = symbol.substring(1, symbol.length());
        }

        switch (symbol) {
            case "B" -> symbol = "byte";
            case "S" -> symbol = "short";
            case "I" -> symbol = "int";
            case "J" -> symbol = "long";
            case "F" -> symbol = "float";
            case "D" -> symbol = "double";
            case "Z" -> symbol = "boolean";
            case "C" -> symbol = "char";
            case "V" -> symbol = "void";
            default -> {
            }
        }

        return symbol;
    }

    public Integer getCompilationLevel() {
        return compilationLevel;
    }

    public void setCompilationLevel(Integer compilationLevel) {
        this.compilationLevel = compilationLevel;
    }

    @Override
    public boolean isTrained() {
        return true;
    }

    @Override
    public boolean isTraineable() {
        return true;
    }

    public CodeObject getAdapter() {
        return adapter;
    }

    @Override
    public AttributedString getDescription(String leftPadding, Boolean verbose, Boolean tips) {
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.append(super.getDescription(leftPadding, verbose, tips));
        if (verbose) {
            sb.append(AttributedString.NEWLINE);
            sb.append(leftPadding).append("This is the compiled code of the method ");
            sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
            sb.append(method.getKey());
            sb.style(AttributedStyle.DEFAULT);
            sb.append(".");
        }

        sb.append(AttributedString.NEWLINE);
        sb.append(leftPadding).append("Compilation Level: ");
        sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
        sb.append("" + this.getCompilationLevel());
        sb.style(AttributedStyle.DEFAULT);
        sb.append(AttributedString.NEWLINE);

        if (verbose) {
            sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT));
            sb.append(leftPadding).append("  ℹ\uFE0F  Higher compilation levels mean a more optimized compilation.");
            sb.append(AttributedString.NEWLINE);
        }
        if (tips) {
            sb.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
            sb.append(leftPadding).append("  \uD83D\uDCA1  Key methods should aim for compilation 3 or above.");
            sb.append(AttributedString.NEWLINE);
        }
        sb.style(AttributedStyle.DEFAULT);
        if (verbose) {
            if (this.adapter != null) {
                sb.append(leftPadding).append("The adapter used to call this method is ");
                sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.CYAN));
                sb.append(this.adapter.getKey());
                sb.style(AttributedStyle.DEFAULT);
            } else {
                sb.append(leftPadding).append("The adapter signature that would be used to call this method is '");
                sb.style(AttributedStyle.DEFAULT.bold().foreground(AttributedStyle.GREEN));
                sb.append(this.getMethod().getAdapterSignature());
                sb.style(AttributedStyle.DEFAULT);
                sb.append(leftPadding).append("'.");
            }
            sb.append(AttributedString.NEWLINE);
        }


        return sb.toAttributedString();
    }

}
