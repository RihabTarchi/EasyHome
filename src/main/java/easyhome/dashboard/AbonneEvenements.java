package easyhome.dashboard;

import easyhome.common.Constantes;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.function.Consumer;


public class AbonneEvenements {

    private final Consumer<String> auRecu;
    private Connection connexion;

    public AbonneEvenements(Consumer<String> auRecu) {
        this.auRecu = auRecu;
    }

    public void demarrer() throws JMSException {
        ActiveMQConnectionFactory usine = new ActiveMQConnectionFactory(Constantes.URL_BROKER);
        connexion = usine.createConnection();
        Session session = connexion.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(Constantes.NOM_TOPIC);
        MessageConsumer consommateur = session.createConsumer(topic);

        consommateur.setMessageListener(message -> {
            if (message instanceof TextMessage) {
                try {
                    auRecu.accept(((TextMessage) message).getText());
                } catch (JMSException e) {
                    System.err.println("JMS : message illisible : " + e.getMessage());
                }
            }
        });

        connexion.start();
    }

    public void arreter() {
        try {
            if (connexion != null) {
                connexion.close();
            }
        } catch (JMSException e) {
        }
    }
}