package app;

import aima.core.environment.wumpusworld.WumpusPercept;
import aima.core.environment.wumpusworld.AgentPosition;

public class WumpusWorld {
    public final int STENCH = 1;
    public final int BREEEZE = 2;
    public final int GLITTER = 4;
    public final int WUMPUS = 8;
    public final int PIT = 16;
    private int record = 0;

    private final int[][] rooms = new int[6][6];
    private AgentPosition ap = new AgentPosition(1, 1, AgentPosition.Orientation.FACING_EAST);
    private WumpusPercept futurePercept;

    private boolean agentHasArrow = true;
    private boolean agentHasGold = false;
    private boolean agentAlive = true;
    private boolean inGame = true;

    private final int wumpusX;
    private final int wumpusY;

    public WumpusWorld() {
        this(1, 3, 2, 3, new int[]{3, 3, 4}, new int[]{1, 3, 4});
    }

    public WumpusWorld(int wumpusX, int wumpusY, int goldX, int goldY, int[] pitX, int[] pitY) {
        this.wumpusX = wumpusX;
        this.wumpusY = wumpusY;
        setWumpus(wumpusX, wumpusY);
        setGold(goldX, goldY);
        for (int i = 0; i < pitX.length; i++)
            setPit(pitX[i], pitY[i]);

        futurePercept = new WumpusPercept();

        if (checkBreeze(ap.getX(), ap.getY()))
            futurePercept.setBreeze();

        if (checkStench(ap.getX(), ap.getY()))
            futurePercept.setStench();

        if (checkGlitter(ap.getX(), ap.getY()))
            futurePercept.setGlitter();
    }

    private void setPit(int i, int j) {
        rooms[i][j] += PIT;
        rooms[i - 1][j] += BREEEZE;
        rooms[i + 1][j] += BREEEZE;
        rooms[i][j - 1] += BREEEZE;
        rooms[i][j + 1] += BREEEZE;
    }

    private void setGold(int i, int j) {
        rooms[i][j] += GLITTER;
    }

    private void setWumpus(int i, int j) {
        rooms[i][j] += WUMPUS;
        rooms[i - 1][j] += STENCH;
        rooms[i + 1][j] += STENCH;
        rooms[i][j - 1] += STENCH;
        rooms[i][j + 1] += STENCH;
    }

    public void changeWorld(String action) {
        record -= 1;
        if (!agentAlive || !inGame)
            return;
        futurePercept = new WumpusPercept();
        AgentPosition.Orientation n = ap.getOrientation();
        int caveXY = 4;
        switch (action) {
            case "TurnLeft":
                if (n == AgentPosition.Orientation.FACING_EAST)
                    n = AgentPosition.Orientation.FACING_NORTH;
                else if (n == AgentPosition.Orientation.FACING_NORTH)
                    n = AgentPosition.Orientation.FACING_WEST;
                else if (n == AgentPosition.Orientation.FACING_WEST)
                    n = AgentPosition.Orientation.FACING_SOUTH;
                else if (n == AgentPosition.Orientation.FACING_SOUTH)
                    n = AgentPosition.Orientation.FACING_EAST;
                ap = new AgentPosition(ap.getRoom(), n);
                break;
            case "TurnRight":
                if (n == AgentPosition.Orientation.FACING_NORTH)
                    n = AgentPosition.Orientation.FACING_EAST;
                else if (n == AgentPosition.Orientation.FACING_WEST)
                    n = AgentPosition.Orientation.FACING_NORTH;
                else if (n == AgentPosition.Orientation.FACING_SOUTH)
                    n = AgentPosition.Orientation.FACING_WEST;
                else if (n == AgentPosition.Orientation.FACING_EAST)
                    n = AgentPosition.Orientation.FACING_SOUTH;
                ap = new AgentPosition(ap.getRoom(), n);
                break;
            case "Forward":
                if (n == AgentPosition.Orientation.FACING_EAST) {
                    if (ap.getX() < caveXY)
                        ap = new AgentPosition(ap.getX() + 1, ap.getY(), ap.getOrientation());
                    else
                        futurePercept.setBump();
                } else if (n == AgentPosition.Orientation.FACING_NORTH) {
                    if (ap.getY() < caveXY)
                        ap = new AgentPosition(ap.getX(), ap.getY() + 1, ap.getOrientation());
                    else
                        futurePercept.setBump();
                } else if (n == AgentPosition.Orientation.FACING_WEST) {
                    if (ap.getX() > 1)
                        ap = new AgentPosition(ap.getX() - 1, ap.getY(), ap.getOrientation());
                    else
                        futurePercept.setBump();
                } else if (n == AgentPosition.Orientation.FACING_SOUTH) {
                    if (ap.getX() > 1)
                        ap = new AgentPosition(ap.getX(), ap.getY() - 1, ap.getOrientation());
                    else
                        futurePercept.setBump();
                }
                if (checkPIT(ap.getX(), ap.getY()) || checkWumpus(ap.getX(), ap.getY())) {
                    agentAlive = false;
                    inGame = false;
                    record -= 1000;
                }
                break;
            case "Shoot":
                if (!agentHasArrow)
                    throw new IllegalStateException("Agent don't have an arrow!");
                else {
                    record -= 9;
                    agentHasArrow = false;
                    boolean killed = false;
                    if (n == AgentPosition.Orientation.FACING_NORTH) {
                        if (wumpusY > ap.getY() && wumpusX == ap.getX())
                            killed = true;
                    } else if (n == AgentPosition.Orientation.FACING_WEST) {
                        if (wumpusY == ap.getY() && wumpusX < ap.getX())
                            killed = true;
                    } else if (n == AgentPosition.Orientation.FACING_SOUTH) {
                        if (wumpusY < ap.getY() && wumpusX == ap.getX())
                            killed = true;
                    } else if (n == AgentPosition.Orientation.FACING_EAST) {
                        if (wumpusY == ap.getY() && wumpusX > ap.getX())
                            killed = true;
                    }
                    if (killed) {
                        futurePercept.setScream();
                    }
                }
                break;
            case "Climb":
                inGame = false;
                if (agentHasGold)
                    record += 1000;
                break;
            case "Grab":
                if (checkGlitter(ap.getX(), ap.getY())) {
                    rooms[ap.getX()][ap.getY()] -= GLITTER;
                    agentHasGold = true;
                } else {
                    throw new IllegalStateException("At this room there isn't any gold!");
                }
                break;
        }

        if (checkBreeze(ap.getX(), ap.getY()))
            futurePercept.setBreeze();

        if (checkStench(ap.getX(), ap.getY()))
            futurePercept.setStench();

        if (checkGlitter(ap.getX(), ap.getY()))
            futurePercept.setGlitter();
    }

    private boolean checkBreeze(int x, int y) {
        return (rooms[x][y] / BREEEZE) % 2 == 1;
    }

    private boolean checkStench(int x, int y) {
        return (rooms[x][y] / STENCH) % 2 == 1;
    }

    private boolean checkGlitter(int x, int y) {
        return (rooms[x][y] / GLITTER) % 2 == 1;
    }

    private boolean checkPIT(int x, int y) {
        return (rooms[x][y] / PIT) % 2 == 1;
    }

    private boolean checkWumpus(int x, int y) {
        return (rooms[x][y] / WUMPUS) % 2 == 1;
    }

    public WumpusPercept getPercept() {
        return futurePercept;
    }

    public int getRecord() {
        return record;
    }
}