package tooling.leyden;

public class QuarkusPicocliLineAppTest {

    public static void printStatusMessages() {
        QuarkusPicocliLineApp.statusMessages.stream().forEachOrdered(
                statusMessage ->
                        System.out.println(statusMessage.timestamp() + " :: " + statusMessage.message())
        );

        QuarkusPicocliLineApp.statusMessages.clear();
    }

}
