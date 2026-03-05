package tooling.leyden.commands.autocomplete;

import java.util.Iterator;

import tooling.leyden.aotcache.Information;

public class Types implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Information.getMyself().getAllTypes().iterator();
    }
}
