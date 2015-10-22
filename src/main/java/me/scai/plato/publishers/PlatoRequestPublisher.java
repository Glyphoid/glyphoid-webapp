package me.scai.plato.publishers;

import com.google.gson.JsonObject;

import java.net.InetAddress;

public interface PlatoRequestPublisher {
    public String publishCreateEngineRequest(JsonObject reqObj,
                                             InetAddress clientIPAddress,
                                             String clientHostName,
                                             String engineId);
    public String publishRemoveEngineRequest(JsonObject reqObj);
    public String publishGeneralRequest(JsonObject reqObj);
    public void destroy();
}
