package tooling.leyden.commands.autocomplete;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tooling.leyden.aotcache.Information;

import java.util.Iterator;


@ApplicationScoped
public class Types implements Iterable<String> {

	@Inject
	Information information;

	@Override
	public Iterator<String> iterator() {
		return information.getAllTypes().iterator();
	}
}
