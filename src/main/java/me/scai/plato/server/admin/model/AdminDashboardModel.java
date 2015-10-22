package me.scai.plato.server.admin.model;

import me.scai.utilities.WorkerClientInfo;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by scai on 4/18/2015.
 */


public class AdminDashboardModel {
    private int currNumWorkers;       /* Current number of workers */
    private int numEverCreatedWorkers;/* Total number of workers ever created */
    private int numPurgedWorkers;     /* Number of workers that have been purged */
    private int numNormallyRemovedWorkers; /* Number of workers that have been normally removed */
    private int maxNumWorkers;        /* Maximum allowed number of workers */
    private long workerTimeoutMillis; /* Worker timeout in milliseconds */
    private LinkedList<WorkerAndClientInfo> workersAndClientsInfo;
    private LinkedList<String> workerIDs;      /* UUIDs of the workers */
    private LinkedList<String> workerClientIPAddresses; /* IP addresses of the clients of the workers */

    /* Getters */
    public int getCurrNumWorkers() {
        return currNumWorkers;
    }

    public int getMaxNumWorkers() {
        return maxNumWorkers;
    }

    public long getWorkerTimeoutMillis() {
        return workerTimeoutMillis;
    }

    public LinkedList<String> getWorkerIDs() {
        return workerIDs;
    }

    public LinkedList<String> getWorkerClientIPAddresses() {
        return workerClientIPAddresses;
    }

    public LinkedList<WorkerAndClientInfo> getWorkersAndClientsInfo() {
        return workersAndClientsInfo;
    }

    public int getNumEverCreatedWorkers() {
        return numEverCreatedWorkers;
    }

    public int getNumPurgedWorkers() {
        return numPurgedWorkers;
    }

    public int getNumNormallyRemovedWorkers() {
        return numNormallyRemovedWorkers;
    }

    /* Setters */
    public void setCurrNumWorkers(int currNumWorkers) {
        this.currNumWorkers = currNumWorkers;
    }

    public void setMaxNumWorkers(int maxNumWorkers) {
        this.maxNumWorkers = maxNumWorkers;
    }

    public void setWorkerTimeoutMillis(long workerTimeoutMillis) {
        this.workerTimeoutMillis = workerTimeoutMillis;
    }

    public void setWorkerIDs(LinkedList<String> workerIDs) {
        this.workerIDs = workerIDs;
    }

    public void setWorkerClientIPAddresses(LinkedList<String> workerClientIPAddresses) {
        this.workerClientIPAddresses = workerClientIPAddresses;
    }

    public void setWorkersAndClientsInfo(LinkedList<WorkerAndClientInfo> workersAndClientsInfo) {
        this.workersAndClientsInfo = workersAndClientsInfo;
    }

    public void setNumEverCreatedWorkers(int numEverCreatedWorkers) {
        this.numEverCreatedWorkers = numEverCreatedWorkers;
    }

    public void setNumPurgedWorkers(int numPurgedWorkers) {
        this.numPurgedWorkers = numPurgedWorkers;
    }

    public void setNumNormallyRemovedWorkers(int numNormallyRemovedWorkers) {
        this.numNormallyRemovedWorkers = numNormallyRemovedWorkers;
    }
}
