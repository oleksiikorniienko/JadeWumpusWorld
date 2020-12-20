package app;

import aima.core.environment.wumpusworld.WumpusPercept;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;

public class EnvironmentAgent extends Agent {
    private AID speleologistAgent;
    private WumpusWorld wumpusWorld;

    @Override
    protected void setup() {
        System.out.println("Environment agent " + getAID().getName() + " is ready!");

        wumpusWorld = new WumpusWorld();
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Wumpus-World-Environment");
        sd.setName("Environment-wandering");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new RequestBehavior());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment agent terminated.");
    }

    private class RequestBehavior extends CyclicBehaviour {
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null) {
                if (speleologistAgent == null)
                    speleologistAgent = msg.getSender();
                if (speleologistAgent.equals(msg.getSender())) {
                    if (msg.getPerformative() == ACLMessage.REQUEST)
                        myAgent.addBehaviour(new PerceptReplyBehaviour(msg));
                    if (msg.getPerformative() == ACLMessage.CFP)
                        myAgent.addBehaviour(new WorldChangingBehaviour(msg));
                } else {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                    myAgent.send(reply);
                }
            } else {
                block();
            }
        }
    }

    private class PerceptReplyBehaviour extends OneShotBehaviour {
        ACLMessage msg;

        public PerceptReplyBehaviour(ACLMessage m) {
            super();
            msg = m;
        }

        public void action() {
            ACLMessage reply = msg.createReply();
            reply.setPerformative(ACLMessage.INFORM);
            reply.setContent(generateSequence());
            myAgent.send(reply);
            System.out.println(getAID().getLocalName() + ": " + reply.getContent());
        }

        private String generateSequence() {
            StringBuilder reply = new StringBuilder();
            reply.append("[");
            WumpusPercept ap = wumpusWorld.getPercept();
            if (ap.isStench())
                reply.append("Here it stinks. "); // Stench in this room everywhere.
            if (ap.isBreeze())
                reply.append("The wind blows. "); // Breeze
            if (ap.isGlitter())
                reply.append("Something flashed. ");
            if (ap.isBump())
                reply.append("Bump. ");
            if (ap.isScream())
                reply.append("I hear a scream.");
            return reply.toString();
        }
    }

    private class WorldChangingBehaviour extends OneShotBehaviour {
        ACLMessage msg;

        ArrayList<String> ForwardKeyWords = new ArrayList<>();
        ArrayList<String> ShootKeyWords = new ArrayList<>();
        ArrayList<String> ClimbKeyWords = new ArrayList<>();
        ArrayList<String> GrabKeyWords = new ArrayList<>();
        ArrayList<String> RightKeyWords = new ArrayList<>();
        ArrayList<String> LeftKeyWords = new ArrayList<>();

        public WorldChangingBehaviour(ACLMessage m) {
            super();
            msg = m;

            ForwardKeyWords.add("forward");
            ForwardKeyWords.add("ahead");
            ForwardKeyWords.add("before");
            ForwardKeyWords.add("along");

            ShootKeyWords.add("shoot");
            ShootKeyWords.add("fire");
            ShootKeyWords.add("gun");

            ClimbKeyWords.add("climb");
            ClimbKeyWords.add("rise");
            ClimbKeyWords.add("lift");

            GrabKeyWords.add("grab");
            GrabKeyWords.add("take");
            GrabKeyWords.add("capture");

            RightKeyWords.add("right");

            LeftKeyWords.add("left");
        }

        public void action() {
            String content = msg.getContent().toLowerCase();

            for (String forward : ForwardKeyWords) {
                if (content.toLowerCase().contains(forward)) {
                    wumpusWorld.changeWorld("Forward");
                    return;
                }
            }

            for (String shoot : ShootKeyWords) {
                if (content.toLowerCase().contains(shoot)) {
                    wumpusWorld.changeWorld("Shoot");
                    return;
                }
            }

            for (String climb : ClimbKeyWords) {
                if (content.toLowerCase().contains(climb)) {
                    wumpusWorld.changeWorld("Climb");
                    return;
                }
            }

            for (String grab : GrabKeyWords) {
                if (content.toLowerCase().contains(grab)) {
                    wumpusWorld.changeWorld("Grab");
                    return;
                }
            }

            for (String right : RightKeyWords) {
                if (content.toLowerCase().contains(right)) {
                    wumpusWorld.changeWorld("TurnRight");
                    return;
                }
            }

            for (String left : LeftKeyWords) {
                if (content.toLowerCase().contains(left)) {
                    wumpusWorld.changeWorld("TurnLeft");
                    return;
                }
            }
        }
    }
}