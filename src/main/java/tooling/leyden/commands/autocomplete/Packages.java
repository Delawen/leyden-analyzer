package tooling.leyden.commands.autocomplete;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import tooling.leyden.aotcache.Information;

import java.util.Iterator;


@Dependent
public class Packages implements Iterable<String> {

	@Inject
	Information information;

	@Override
	public Iterator<String> iterator() {
		return information.getAllPackages().iterator();
	}
}
