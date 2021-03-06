package openjade.core.behaviours;

import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.onto.Ontology;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.lang.reflect.Method;

import openjade.core.OpenAgent;
import openjade.core.annotation.ReceiveMatchMessage;
import openjade.core.annotation.ReceiveSignerMessage;
import openjade.core.behaviours.BehaviourException;

import org.apache.log4j.Logger;

public class ReceiveOntologyMessageBehaviour extends CyclicBehaviour {

	private OpenAgent myAgent;

	private static final long serialVersionUID = 1L;

	protected static Logger log = Logger.getLogger(ReceiveOntologyMessageBehaviour.class);

	public ReceiveOntologyMessageBehaviour(Agent _agent) {
		super(_agent);
		myAgent = (OpenAgent) _agent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void action() {
		ACLMessage message = myAgent.receiveMessage();
		if (message != null) {
			try {
				Method[] methods = myAgent.getClass().getMethods();
				for (Method method : methods) {
					method.setAccessible(true);
					if (method.isAnnotationPresent(ReceiveSignerMessage.class)) {
						method.invoke(myAgent, message);
						return;
					}
					if (method.isAnnotationPresent(ReceiveMatchMessage.class)) {
						ReceiveMatchMessage messageMatch = method.getAnnotation(ReceiveMatchMessage.class);
						Method getInstance = messageMatch.ontology().getMethod("getInstance");
						getInstance.setAccessible(true);
						Ontology ontology = (Ontology) getInstance.invoke(null);
						Codec codec = (Codec) messageMatch.codec().newInstance();
						MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchLanguage(codec.getName()), MessageTemplate.MatchOntology(ontology.getName()));
						if (messageTemplate.match(message)) {
							ContentElement ce = myAgent.extractContent(message, codec, ontology);
							if (messageMatch.action().isInstance(ce)) {
								int[] performatives = messageMatch.performative();
								for (int performative : performatives) {
									if (message.getPerformative() == performative) {
										method.invoke(myAgent, message, ce);
										return;
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				throw new BehaviourException(e.getMessage(), e);
			}
		}
	}
}
