package app;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class SpeleologistAgent extends Agent {
    private static final int TIMEOUT_DELAY = 5000;

    private AID navigatorId;
    private AID environmentId;

    @Override
    protected void setup() {
        System.out.println("Speleologist agent " + getAID().getLocalName() + " is ready!");
        addBehaviour(findAndSetAgents());
    }

    @Override
    protected void takeDown() {
        System.out.println("Speleologist agent terminating...");
    }

    private WakerBehaviour findAndSetAgents() {
        return new WakerBehaviour(this, TIMEOUT_DELAY) {
            @Override
            protected void onWake() {
                DFAgentDescription navigatorDescription = new DFAgentDescription();
                DFAgentDescription environmentDescription = new DFAgentDescription();
                ServiceDescription navigatorService = new ServiceDescription();
                ServiceDescription environmentService = new ServiceDescription();
                navigatorService.setType("Wumpus-World-Navigator");
                environmentService.setType("Wumpus-World-Environment");
                navigatorDescription.addServices(navigatorService);
                environmentDescription.addServices(environmentService);

                try {
                    navigatorId = DFService.search(myAgent, navigatorDescription)[0].getName();
                    environmentId = DFService.search(myAgent, environmentDescription)[0].getName();
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                myAgent.addBehaviour(new EnvironmentWanderingBehaviour());
            }
        };
    }

    private class EnvironmentWanderingBehaviour extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        private String message;

        @Override
        public void action() {
            switch (step) {
                case 0 -> makeRequestToEnvironment();
                case 1 -> saveEnvironmentState();
                case 2 -> makeRequestToNavigator();
                case 3 -> getNavigatorResponse();
                case 4 -> makeActionAndSendToEnvironment();
                case 5 -> isAlreadyClimbed();
            }
        }

        private void makeRequestToEnvironment() {
            ACLMessage requestPercept = new ACLMessage(ACLMessage.REQUEST);
            requestPercept.addReceiver(environmentId);
            requestPercept.setConversationId("percept");
            myAgent.send(requestPercept);
            System.out.println(getAID().getLocalName() + ": Gathering information about Environment.");
            mt = MessageTemplate.MatchConversationId("percept");

            step++;
        }

        private void saveEnvironmentState() {
            ACLMessage reply = myAgent.receive(mt);
            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.INFORM) {
                    message = reply.getContent().concat("What do I need to do?");
                    step++;
                }
            } else
                block();
        }

        private void makeRequestToNavigator() {
            ACLMessage askForAction = new ACLMessage(ACLMessage.REQUEST);
            askForAction.addReceiver(navigatorId);
            askForAction.setContent(message);
            askForAction.setConversationId("Ask-for-action");
            myAgent.send(askForAction);
            System.out.println(getAID().getLocalName() + ": " + message);
            mt = MessageTemplate.MatchConversationId("Ask-for-action");
            step++;
        }

        private void getNavigatorResponse() {
            ACLMessage reply2 = myAgent.receive(mt);
            if (reply2 != null) {
                if (reply2.getPerformative() == ACLMessage.PROPOSE) {
                    message = reply2.getContent();
                    step++;
                }
            } else {
                block();
            }
        }

        private void makeActionAndSendToEnvironment() {
            ACLMessage action = new ACLMessage(ACLMessage.CFP);
            action.addReceiver(environmentId);
            action.setContent(message);
            action.setConversationId("action");
            myAgent.send(action);
            System.out.println(getAID().getLocalName() + ": " + message);
            mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId("action"),
                    MessageTemplate.MatchInReplyTo(action.getReplyWith()));
            step++;
        }

        private void isAlreadyClimbed() {
            if (message.equals("Climb")) {
                step++;
                doDelete();
            } else {
                step = 0;
            }
        }

        @Override
        public boolean done() {
            return step == 6;
        }
    }
}

