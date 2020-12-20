package app;

import aima.core.agent.impl.DynamicAction;
import aima.core.environment.wumpusworld.*;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class NavigatorAgent extends Agent {
    private AID speleologistAgent;
    private HybridWumpusAgent agentLogic;

    @Override
    protected void setup() {
        agentLogic = new HybridWumpusAgent();

        System.out.println("Navigator agent " + getAID().getLocalName() + " is ready!");
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Wumpus-World-Navigator");
        sd.setName("Wumpus-Gold-finder");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new PerceptRequestBehavior());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator agent terminated.");
    }

    private class PerceptRequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("Ask-for-action")));
            if (msg != null) {
                if (speleologistAgent == null)
                    speleologistAgent = msg.getSender();
                if (speleologistAgent.equals(msg.getSender()))
                    myAgent.addBehaviour(new CreateSendPropose(msg));
                else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
            } else {
                block();
            }
        }
    }

    private class CreateSendPropose extends OneShotBehaviour {
        ACLMessage msg;

        ArrayList<String> StenchKeyWords = new ArrayList<>();
        ArrayList<String> BreezeKeyWords = new ArrayList<>();
        ArrayList<String> GlitterKeyWords = new ArrayList<>();
        ArrayList<String> BumpKeyWords = new ArrayList<>();
        ArrayList<String> ScreamKeyWords = new ArrayList<>();

        public CreateSendPropose(ACLMessage m) {
            super();
            msg = m;
            StenchKeyWords.add("stink");
            StenchKeyWords.add("stench");
            StenchKeyWords.add("reek");
            StenchKeyWords.add("pong");
            StenchKeyWords.add("funk");

            BreezeKeyWords.add("breeze");
            BreezeKeyWords.add("wind");
            BreezeKeyWords.add("gale");
            BreezeKeyWords.add("air");

            GlitterKeyWords.add("glitter");
            GlitterKeyWords.add("flash");
            GlitterKeyWords.add("brilliance");

            BumpKeyWords.add("bump");
            BumpKeyWords.add("strike");
            BumpKeyWords.add("hit");
            BumpKeyWords.add("bounce");

            ScreamKeyWords.add("scream");
            ScreamKeyWords.add("yell");
            ScreamKeyWords.add("bellow");
            ScreamKeyWords.add("wail");
        }

        public void action() {
            String content = msg.getContent();
            ACLMessage reply = msg.createReply();
            if (content != null) {
                reply.setPerformative(ACLMessage.PROPOSE);
                DynamicAction action = (DynamicAction) agentLogic.execute(extractPercept(content));
                reply.setContent(action.getName());
            } else {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("not-available");
            }
            myAgent.send(reply);
            System.out.println(getAID().getLocalName() + ": " + reply.getContent());
        }

        private WumpusPercept extractPercept(String content) {
            content = content.toLowerCase();
            WumpusPercept wumpusPercept = new WumpusPercept();

            for (String stench : StenchKeyWords) {
                if (content.toLowerCase().contains(stench)) {
                    wumpusPercept.setStench();
                    break;
                }
            }

            for (String breeze : BreezeKeyWords) {
                if (content.toLowerCase().contains(breeze)) {
                    wumpusPercept.setBreeze();
                    break;
                }
            }

            for (String glitter : GlitterKeyWords) {
                if (content.toLowerCase().contains(glitter)) {
                    wumpusPercept.setGlitter();
                    break;
                }
            }

            for (String bump : BumpKeyWords) {
                if (content.toLowerCase().contains(bump)) {
                    wumpusPercept.setBump();
                    break;
                }
            }

            for (String scream : ScreamKeyWords) {
                if (content.toLowerCase().contains(scream)) {
                    wumpusPercept.setScream();
                    break;
                }
            }

            return wumpusPercept;
        }
    }
}