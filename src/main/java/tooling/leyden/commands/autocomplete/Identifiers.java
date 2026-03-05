package tooling.leyden.commands.autocomplete;

import java.util.Iterator;

import tooling.leyden.aotcache.Information;

public class Identifiers implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Information.getMyself().getIdentifiers().iterator();
    }
}
