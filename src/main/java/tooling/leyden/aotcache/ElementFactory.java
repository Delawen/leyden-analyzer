package tooling.leyden.aotcache;

import tooling.leyden.commands.CommonParameters;

public class ElementFactory {

    public static Element getOrCreate(String identifier, String type, String address, Playground playground) {
        CommonParameters params = new CommonParameters();
        params.setUse(CommonParameters.ElementsToUse.both);
        params.setName(identifier);
        params.setTypes(new String[]{type});
        return Information.getMyself()
                .getElements(params, playground)
                .findAny()
                .orElseGet(() -> getElement(identifier, type, address, playground));
    }

    private static Element getElement(String identifier, String type, String address, Playground playground) {
        Element e;

        if (address != null && !type.equals("PlaceHolder") && Information.getMyself().getByAddress(address, playground) != null) {
            Information.getMyself().addWarning(Information.getMyself().getByAddress(address, playground),
                    "Address collision at " + address, WarningType.CacheCreation);
        }

        switch (type) {
            case "Class" -> e = new ClassObject(identifier);
            case "Method" -> e = new MethodObject(identifier, playground);
            case "ConstantPool" -> e = new ConstantPoolObject(identifier);
            case "KlassTrainingData", "CompileTrainingData", "MethodData", "MethodCounters", "MethodTrainingData",
                    "Symbol" -> e = new ReferencingElement(identifier, type);
            case "Object" -> e = new InstanceObject(identifier);
            case "Nmethod" -> e = new NMethodObject(identifier);
            case "StubGenBlob", "SharedBlob", "C1Blob", "C2Blob",
                 "Adapter", "EmbeddedStub" -> e = new CodeObject(identifier, type);
            case "PlaceHolder" -> {
                e = Information.getMyself().getByAddress(address, playground);
                if (e == null) {
                    e = new PlaceHolderElement(address);
                    Information.getMyself().addPlaceholder((PlaceHolderElement) e);
                }
            }
            default -> {
                e = new BasicObject(identifier);
                e.setType(type);
            }
        }

        e.setPlayground(playground);

        if (address != null) {
            e.setAddress(address);
        }

        e = Information.getMyself().updateElement(e);

        if (! (e instanceof PlaceHolderElement) && e.getAddress() != null) {
             PlaceHolderElement placeholder = Information.getMyself().getPlaceholder(e.getAddress());
            if (placeholder != null) {
                Information.getMyself().replacePlaceHolder(placeholder, e);
            }
        }

        return e;
    }
}
