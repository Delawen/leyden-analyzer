package tooling.leyden.commands.autocomplete;

import java.util.Iterator;

import tooling.leyden.aotcache.Information;

public class Addressess implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return Information.getMyself().getAddressess().iterator();
    }
}
