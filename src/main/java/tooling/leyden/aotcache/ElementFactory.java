package tooling.leyden.aotcache;

public class ElementFactory {

    public static Element getOrCreate(String identifier, String type, String address) {
        return Information.getMyself()
                .getElements(identifier, null, null, true, true, type)
                .findAny()
                .orElseGet(() -> getElement(identifier, type, address));
    }

    private static Element getElement(String identifier, String type, String address) {
        Element e;

        if (address != null && Information.getMyself().getByAddress(address) != null) {
            Information.getMyself().addWarning(Information.getMyself().getByAddress(address),
                    "Address collision at " + address, WarningType.CacheCreation);
        }

        switch (type) {
            case "Class" -> e = new ClassObject(identifier);
            case "Method" -> e = new MethodObject(identifier);
            case "ConstantPool" -> e = new ConstantPoolObject(identifier);
            case "KlassTrainingData", "CompileTrainingData", "MethodData", "MethodCounters", "MethodTrainingData",
                    "Symbol" -> e = new ReferencingElement(identifier, type);
            case "Object" -> e = new InstanceObject(identifier);
            case "Nmethod" -> e = new NMethodObject(identifier);
            case "StubGenBlob", "SharedBlob", "C1Blob", "C2Blob",
                 "Adapter", "EmbeddedStub" -> e = new CodeObject(identifier, type);
            default -> {
                e = new BasicObject(identifier);
                e.setType(type);
            }
        }

        //By default, all elements go here
        Information.getMyself().addExternalElement(e);
        //When we mark them as saved in the cache, we will move them from here

        if (address != null) {
            e.setAddress(address);
        }

        return e;
    }
}
