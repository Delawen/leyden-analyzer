package tooling.leyden.commands;

import picocli.CommandLine;
import tooling.leyden.commands.autocomplete.*;

@CommandLine.Command(synopsisHeading = "%nUsage:%n%n", descriptionHeading = "%nDescription:%n%n", parameterListHeading = "%nParameters:%n%n", optionListHeading = "%nOptions:%n%n", commandListHeading = "%nCommands:%n%n")
public class CommonParameters {

    public enum ElementsToUse {
        cached,
        notCached,
        both
    }

    @CommandLine.Option(names = { "-pn", "--packageName" }, description = {
            "Restrict the command to elements inside this package. ",
            "Note that some elements don't belong to any particular package" }, arity = "0..*", split = ",", paramLabel = "<packageName>", completionCandidates = Packages.class)
    protected String[] packageName;

    @CommandLine.Option(names = { "-epn", "--excludePackageName" }, description = {
            "Exclude the elements inside this package. ",
            "Note that some elements don't belong to any particular package." }, arity = "0..*", split = ",", paramLabel = "<exclude>", completionCandidates = Packages.class)
    protected String[] excludePackageName;

    @CommandLine.Option(names = { "--identifier", "-i" }, description = {
            "The object identifier. If it is a class, use the full qualified name." }, defaultValue = "", arity = "0..1", paramLabel = "<id>", completionCandidates = Identifiers.class)
    private String name;

    @CommandLine.Option(names = { "--like" }, description = {
            "A regexp to compare the identifier." }, defaultValue = "", arity = "0..1", paramLabel = "<like>", completionCandidates = Identifiers.class)
    private String nameLike;

    @CommandLine.Option(names = { "--address", "-a" }, description = {
            "Find elements on this address (0x....). It has to be the long address." }, arity = "0..1", paramLabel = "<address>", completionCandidates = Addressess.class)
    String address;

    @CommandLine.Option(names = {
            "--showArrays" }, description = "Display array classes if true. True by default.", defaultValue = "true", negatable = true, arity = "0..1")
    protected Boolean arrays = true;

    @CommandLine.Option(names = { "--use",
            "-u" }, description = "What type of elements to use on this command: cached during AOT, not cached, or both. " +
                    "By default, shows everything.", defaultValue = "both", arity = "0..1")
    protected ElementsToUse use = ElementsToUse.cached;

    @CommandLine.Option(names = { "-t",
            "--type" }, arity = "0..*", split = ",", paramLabel = "<type>", description = "Restrict the command to this type of element", completionCandidates = Types.class)
    protected String[] types;

    @CommandLine.Option(names = { "-hr",
            "--showHeapRoot" }, arity = "0..1", negatable = true, paramLabel = "<isHeapRoot>", description = "If true, shows only heapRoot elements. If false, shows only non-heapRoot elements.")
    protected Boolean isHeapRoot = null;

    @CommandLine.Option(names = { "--loaded" }, description = {
            "Display classes that were loaded in a training run, a production run, both, or none.",
            "This will restrict types to only classes, regardless of the rest of the arguments." }, defaultValue = "all", arity = "0..1")
    protected WhichRun loaded = WhichRun.all;

    @CommandLine.Option(names = { "--referencing" }, description = {
            "Display elements which reference the element defined by this id." }, arity = "0..1", completionCandidates = Identifiers.class)
    protected String referencing;

    @CommandLine.Option(names = { "--instanceOf" }, description = {
            "Display object instances from this Java Class." }, arity = "0..1", completionCandidates = Identifiers.class)
    protected String instanceOf;

    @CommandLine.Option(names = { "--trained" },
            description = { "Only displays elements with training information (true) or without training information (false).",
            "This may restrict the types of elements shown, along with what was passed as parameters." }, negatable = true, arity = "0..1")
    protected Boolean trained;

    @CommandLine.Option(names = { "--lambdas" }, description = {
            "Display lambda classes too. Useful to hide lambda classes." }, defaultValue = "true", negatable = true, arity = "0..1")
    protected Boolean lambdas = true;

    @CommandLine.Option(names = { "--innerClasses" }, description = {
            "Display inner classes too. Useful to hide inner classes." }, defaultValue = "true", negatable = true, arity = "0..1")
    protected Boolean innerClasses = true;

    @CommandLine.Option(names = {
            "--showAOTInited" }, arity = "0..1", negatable = true, paramLabel = "<aot-inited>", description = "If true, shows only aot-inited objects. If false, shows only non-aot-inited objects.")
    protected Boolean showAOTInited = null;

    @CommandLine.Option(names = { "-v",
            "--verbose" }, arity = "0..1", negatable = true, description = "Display more information.", defaultValue = "false")
    protected Boolean verbose;

    @CommandLine.Option(names = {
            "-hints" }, arity = "0..1", negatable = true, description = "Display hints on how to explore further.", defaultValue = "false")
    protected Boolean hints;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean useArrays() {
        return arrays;
    }

    public void setUseArrays(Boolean arrays) {
        this.arrays = arrays;
    }

    public Boolean isHeapRoot() {
        return isHeapRoot;
    }

    public void setHeapRoot(Boolean heapRoot) {
        isHeapRoot = heapRoot;
    }

    public String[] getPackageName() {
        return packageName;
    }

    public void setPackageName(String[] packageName) {
        this.packageName = packageName;
    }

    public String[] getExcludePackageName() {
        return excludePackageName;
    }

    public void setExcludePackageName(String[] excludePackageName) {
        this.excludePackageName = excludePackageName;
    }

    public String[] getTypes() {
        return types;
    }

    public void setTypes(String[] types) {
        this.types = types;
    }

    public ElementsToUse getUse() {
        return use;
    }

    public void setUse(ElementsToUse use) {
        this.use = use;
    }

    public Boolean getTrained() {
        return trained;
    }

    public Boolean getLambdas() {
        return lambdas;
    }

    public String getReferencing() {
        return referencing;
    }

    public WhichRun getLoaded() {
        return loaded;
    }

    public void setLoaded(WhichRun loaded) {
        this.loaded = loaded;
    }

    public Boolean getInnerClasses() {
        return innerClasses;
    }

    public String getAddress() {
        return address;
    }

    public Boolean getShowAOTInited() {
        return showAOTInited;
    }

    public void setShowAOTInited(Boolean showAOTInited) {
        this.showAOTInited = showAOTInited;
    }

    public String getInstanceOf() {
        return instanceOf;
    }

    public void setInstanceOf(String instanceOf) {
        this.instanceOf = instanceOf;
    }

    public String getNameLike() {
        return nameLike;
    }

    public void setNameLike(String nameLike) {
        this.nameLike = nameLike;
    }
}
