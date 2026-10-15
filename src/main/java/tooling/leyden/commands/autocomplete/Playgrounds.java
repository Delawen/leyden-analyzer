package tooling.leyden.commands.autocomplete;

import tooling.leyden.aotcache.Information;

import java.util.Iterator;

public class Playgrounds implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Information.getMyself().getPlaygrounds().iterator();
    }
}
