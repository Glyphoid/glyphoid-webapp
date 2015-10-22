package me.scai.plato.server.admin.model;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.scai.utilities.WorkerClientInfo;

import java.util.Date;


public class WorkerAndClientInfo {
    private static final Gson gson = new Gson();

    private String workerID;
    private Date createdTimestamp;    /* Time of creation */
    private Date timestamp;           /* Time of last usage */
    private String timeSinceCreation; /* Up time since creation */
    private String timeSinceLastUse;  /* Time since last usage */

    private String prevWorkerID;      /* Previous worker ID */

    private int messageCount;                  /* Number of messages that have been processed */
    private float currentAverageMessageRate;   /* Current message rate: n_M / (t_current - t_creation) */
    private float effectiveAverageMessageRate; /* Effective message rate: n_M / (t_last_message - t_creation) */

    private WorkerClientInfo workerClientInfo;

    private String clientType;
    private String customClientData;

    public WorkerAndClientInfo(String workerID,
                               Date createdTimestamp,
                               Date timestamp,
                               int messageCount,
                               float currentAverageMessageRate,
                               float effectiveAverageMessageRate,
                               WorkerClientInfo workerClientInfo) {
        this.workerID = workerID;
        this.workerClientInfo = workerClientInfo;

        this.createdTimestamp = createdTimestamp;
        this.timestamp        = timestamp;

        this.messageCount                = messageCount;
        this.currentAverageMessageRate   = currentAverageMessageRate;
        this.effectiveAverageMessageRate = effectiveAverageMessageRate;

        Date now = new Date();
        long tSinceCreation = now.getTime() - createdTimestamp.getTime();
        long tSinceLastUse = now.getTime() - timestamp.getTime();

        if (workerClientInfo.getClientTypeMajor() != null &&
            workerClientInfo.getClientTypeMinor() != null) {
            this.clientType = workerClientInfo.getClientTypeMajor().toString() + " / " +
                    workerClientInfo.getClientTypeMinor().toString();
        } else {
            this.clientType = "n/a";
        }

        setTimeSinceCreation(millis2TimeIntervalString(tSinceCreation));
        setTimeSinceLastUse(millis2TimeIntervalString(tSinceLastUse));

        if (workerClientInfo.getCustomClientData() != null) {
            JsonObject customClientData = workerClientInfo.getCustomClientData();

            this.customClientData = gson.toJson(customClientData);
        } else {
            customClientData = "n/a";
        }

    }

    private String millis2TimeIntervalString(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long days    = hours / 24;

        long s = seconds % 60;
        String str = String.format("%d " + (s > 1 ? "seconds" : "second"), s);
        if (millis >= 60 * 1000) {
            long m = minutes % 60;
            str = String.format("%d " + (m > 1 ? "minutes" : "minute") + " %s", m, str);
            if (millis >= 60 * 60 * 1000) {
                long h = hours % 60;
                str = String.format("%d " + (h > 1 ? "minutes" : "minute") + " %s", h, str);
                if (millis >= 24 * 60 * 60 * 1000) {
                    str = String.format("%d " + (days > 1 ? "days" : "day") + " %s", days, str);
                }
            }
        }
//        String str = String.format("%d days %d hours %d minutes %d seconds",
//                days, hours % 24, minutes % 60, seconds % 60);

        return str;
    }

    public String getWorkerID() {
        return workerID;
    }

    public WorkerClientInfo getWorkerClientInfo() {
        return workerClientInfo;
    }

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getTimeSinceCreation() {
        return timeSinceCreation;
    }

    public String getTimeSinceLastUse() {
        return timeSinceLastUse;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public float getCurrentAverageMessageRate() {
        return currentAverageMessageRate;
    }

    public float getEffectiveAverageMessageRate() {
        return effectiveAverageMessageRate;
    }

    public String getPrevWorkerID() {
        return prevWorkerID;
    }

    public void setWorkerID(String workerID) {
        this.workerID = workerID;
    }

    public void setWorkerClientInfo(WorkerClientInfo workerClientInfo) {
        this.workerClientInfo = workerClientInfo;
    }

    public void setCreatedTimestamp(Date createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setTimeSinceCreation(String timeSinceCreation) {
        this.timeSinceCreation = timeSinceCreation;
    }

    public void setTimeSinceLastUse(String timeSinceLastUse) {
        this.timeSinceLastUse = timeSinceLastUse;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public void setEffectiveAverageMessageRate(float effectiveAverageMessageRate) {
        this.effectiveAverageMessageRate = effectiveAverageMessageRate;
    }

    public void setCurrentAverageMessageRate(float currentAverageMessageRate) {
        this.currentAverageMessageRate = currentAverageMessageRate;
    }

    public String getClientType() {
        return clientType;
    }

    public String getCustomClientData() {
        return customClientData;
    }

    public void setPrevWorkerID(String prevWorkerID) {
        this.prevWorkerID = prevWorkerID;
    }
}