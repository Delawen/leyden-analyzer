package tooling.leyden.commands.autocomplete;

import java.util.Iterator;

import tooling.leyden.aotcache.Information;

public class Packages implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Information.getMyself().getAllPackages().iterator();
    }
}
