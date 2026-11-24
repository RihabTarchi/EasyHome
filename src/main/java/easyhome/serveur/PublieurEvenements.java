package easyhome.serveur;

import easyhome.common.Constantes;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

public class PublieurEvenements {

    private Connection connexion;
    private Session session;
    private MessageProducer producteur;

    public void demarrer() throws Exception {
        ActiveMQConnectionFactory usine = new ActiveMQConnectionFactory(Constantes.URL_BROKER);
        connexion = usine.createConnection();
        connexion.start();
        session = connexion.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(Constantes.NOM_TOPIC);
        producteur = session.createProducer(topic);
    }

    public synchronized void publier(String texte) {
        if (producteur == null) {
            return;
        }
        try {
            TextMessage message = session.createTextMessage(texte);
            producteur.send(message);
        } catch (Exception e) {
            System.err.println("JMS : publication impossible : " + e.getMessage());
        }
    }

    public void arreter() {
        try {
            if (connexion != null) {
                connexion.close();
            }
        } catch (Exception e) {
        }
    }
}