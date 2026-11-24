package easyhome.serveur;

import easyhome.common.Constantes;
import org.apache.activemq.broker.BrokerService;

public class BrokerJms {

    private BrokerService broker;

    public void demarrer() throws Exception {
        broker = new BrokerService();
        broker.setBrokerName("easyhome");
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.addConnector(Constantes.URL_BROKER);
        broker.start();
        Journal.ajouter("Broker JMS demarre sur " + Constantes.URL_BROKER);
    }

    public void arreter() {
        try {
            if (broker != null) {
                broker.stop();
            }
        } catch (Exception e) {

        }
    }
}