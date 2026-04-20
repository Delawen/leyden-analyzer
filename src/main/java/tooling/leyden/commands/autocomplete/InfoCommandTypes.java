package tooling.leyden.commands.autocomplete;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class InfoCommandTypes implements Iterable<String> {

    public enum Types {
        Configuration,
        Count,
        Summary
    }

    private final List<String> names = Arrays.stream(Types.values()).map(Enum::name).toList();

    @Override
    public Iterator<String> iterator() {
        return names.iterator();
    }
}
