package tooling.leyden.commands.autocomplete;

import jakarta.inject.Inject;
import tooling.leyden.aotcache.Information;

import java.util.Iterator;


public class Identifiers implements Iterable<String> {

	@Inject
	Information information;

	@Override
	public Iterator<String> iterator() {
		return information.getIdentifiers().iterator();
	}
}
