package tooling.leyden.commands.logparser;

import java.util.function.Consumer;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import tooling.leyden.QuarkusPicocliLineApp;
import tooling.leyden.StatusMessage;
import tooling.leyden.aotcache.Information;
import tooling.leyden.commands.LoadFileCommand;

/**
 * This class is capable of parsing (certain) Java logs.
 */
public abstract class Parser implements Consumer<String> {

    protected final Information information;
    protected final LoadFileCommand loadFile;

    public Parser(LoadFileCommand loadFile) {
        this.information = loadFile.getParent().getInformation();
        this.loadFile = loadFile;
    }

    @Override
    public final void accept(String s) {
        if (!s.isBlank()) {
            try {
                io.quarkus.narayana.jta.QuarkusTransaction.requiringNew().run(() -> actualAccept(s));
            } catch (Exception e) {
                QuarkusPicocliLineApp.addStatusMessage(new StatusMessage(System.currentTimeMillis(),
                        new AttributedString("ERROR: processing line '" + s + "'. " + e.getMessage(),
                                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold())));
            }
        }
    }

    public abstract void actualAccept(String s);

    abstract String getSource();

    public final void postProcessing() {
        io.quarkus.narayana.jta.QuarkusTransaction.joiningExisting().run(() ->  actualPostProcessing());
    }

    public abstract void actualPostProcessing();
}
