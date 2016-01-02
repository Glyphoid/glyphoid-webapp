package me.scai.plato.publishers;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.scai.plato.helpers.PlatoHelper;
import me.scai.plato.serverutils.PropertiesHelper;
import me.scai.utilities.WorkerClientInfo;
import me.scai.utilities.clienttypes.ClientTypeMajor;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PlatoRequestPublisherS3 implements PlatoRequestPublisher {
    /* Helper classes */
    class PlatoRequestPublisherS3Config {
        private String awsS3BucketNameForRequestLogging;
        private String awsAccessKey;
        private String awsSecretKey;

        /* Constructor */
        PlatoRequestPublisherS3Config(String awsS3BucketNameForRequestLogging, String awsAccessKey, String awsSecretKey) {
            this.awsS3BucketNameForRequestLogging = awsS3BucketNameForRequestLogging;
            this.awsAccessKey = awsAccessKey;
            this.awsSecretKey = awsSecretKey;
        }

        /* Getter */
        public String getAwsS3BucketNameForRequestLogging() {
            return awsS3BucketNameForRequestLogging;
        }

        public String getAwsAccessKey() {
            return awsAccessKey;
        }

        public String getAwsSecretKey() {
            return awsSecretKey;
        }
    }

    /* Constants */
    private static final String propFileName = "plato.properties";

    private static final String ENGINE_ID_KEY = "engineUuid";
    private static final String ADDITIONAL_CLIENT_DATA_KEY = "AdditionalClientData";
    private static final String OBJECT_KEY_SUFFIX = ".json";
    private static final Charset CONTENT_ENCODING = StandardCharsets.UTF_8;

    private static final String TIME_ZONE = "UTC";
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final DateFormat s3ObjKeyDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH.mm.ss.SSSZ");

    private static final Gson gson = new Gson();

    private static final Logger logger = Logger.getLogger(PlatoRequestPublisherS3.class.getName());
    private static final ClientTypeMajor[] NO_PUBLISH_CLIENT_TYPE_MAJOR_LIST = {

    };

    /* Member variables */
    private static PlatoRequestPublisherS3 instance;

    private Map<String, WorkerClientInfo> workersClientInfo;
    private ArrayList<ClientTypeMajor> noPublishClientTypeMajorList;

    private AmazonS3Client s3Client;
    private String s3BucketName;

    private ExecutorService exe = Executors.newCachedThreadPool();

    /* Singleton access */
    public static PlatoRequestPublisherS3 createOrGetInstance(Map<String, WorkerClientInfo> workersClientInfo) {
        if (instance == null) {

            instance = new PlatoRequestPublisherS3(workersClientInfo);

        }

        return instance;
    }

    /* Constructors */
    private PlatoRequestPublisherS3(Map<String, WorkerClientInfo> workersClientInfo) {
        PlatoRequestPublisherS3Config config = getPlatoRequestPublisherS3Config();

        /* Set time zone info */
        final TimeZone utcTimeZone = TimeZone.getTimeZone(TIME_ZONE);
        dateFormat.setTimeZone(utcTimeZone);
        s3ObjKeyDateFormat.setTimeZone(utcTimeZone);

        if (workersClientInfo == null) {
            throw new IllegalArgumentException("Received null value in workerClientInfo");
        }

        this.workersClientInfo = workersClientInfo;
        this.s3BucketName      = config.getAwsS3BucketNameForRequestLogging();

        /* Prepare the no-publish client type list */
        noPublishClientTypeMajorList = new ArrayList<ClientTypeMajor>();
        noPublishClientTypeMajorList.ensureCapacity(NO_PUBLISH_CLIENT_TYPE_MAJOR_LIST.length);
        for (ClientTypeMajor noPubType : NO_PUBLISH_CLIENT_TYPE_MAJOR_LIST) {
            noPublishClientTypeMajorList.add(noPubType);
        }

        /* Try to obtain S3 client */
        obtainAwsS3Client(config);

    }

    private void obtainAwsS3Client(PlatoRequestPublisherS3Config config) {
        /* Try IAM role first */
//        boolean s3ClientCreationException = false;
//        try {
//            s3Client = new AmazonS3Client();
//        } catch (Exception exc) {
//            s3ClientCreationException = true;
//        }
//
//        if ( !(s3ClientCreationException || s3Client == null) ) {
//            logger.info("Successfully created S3 client from IAM role");
//            return;
//        }

        /* Then, try using access and secret keys in properties files */
//        logger.info("S3 client creation based on IAM role was not successful. Attempting to use AWS keys in environment variables to create S3 client");
        logger.info("Attempting to use AWS keys in environment variables to create S3 client");

        AWSCredentials cred = new BasicAWSCredentials(config.getAwsAccessKey(), config.getAwsSecretKey());

//        s3Client = new AmazonS3Client(new DefaultAWSCredentialsProviderChain());
        s3Client = new AmazonS3Client(cred);

        if (s3Client == null) {
            throw new RuntimeException("All attempts to obtain S3 client have failed");
        }
    }

    @Override
    public void destroy() {
        exe.shutdown();
        try {
            exe.awaitTermination(10000L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exc) {

        }
    }

    @Override
    public String publishCreateEngineRequest(JsonObject reqObj,
                                             InetAddress clientIPAddress,
                                             String clientHostName,
                                             String engineId) {
        JsonObject additionalClientData = new JsonObject();

        // TODO: Think about thread safety
        additionalClientData.add(ENGINE_ID_KEY, new JsonPrimitive(engineId));
        additionalClientData.add("ClientIPAddress", new JsonPrimitive(clientIPAddress.toString()));
        additionalClientData.add("ClientHostName", new JsonPrimitive(clientHostName));

        reqObj.add(ADDITIONAL_CLIENT_DATA_KEY, additionalClientData);

        String publishedObjKey = publishGeneralRequest(reqObj);

        // Note that publishGeneralRequest is async
//        reqObjWithEngineId.remove(ADDITIONAL_CLIENT_DATA_KEY);

        return publishedObjKey;
    }

    @Override
    public String publishRemoveEngineRequest(JsonObject reqObj) {
        String publishedObjKey = publishGeneralRequest(reqObj);

        return publishedObjKey;
    }

    @Override
    public String publishGeneralRequest(JsonObject reqObj) {
        String engineId = getRequestEngineId(reqObj);
        if (engineId == null) {
            logger.severe("Failed to obtain engine ID from request. Publishing aborted.");
            return null;

        }

        ClientTypeMajor clientTypeMajor = null;
        try {
            clientTypeMajor = engineId2ClientTypeMajor(engineId);
        } catch (IllegalArgumentException exc) {
            return null;
        }

        if (clientTypeMajor == null) {
            logger.severe("Null value in major client type. Publishing aborted.");
            return null;
        }

        if (isPublishBlocked(clientTypeMajor)) {
            return null;
        }

        Date clientTimeCreated = engineId2TimeClientCreated(engineId);
        Date reqTimestmap = getRequestTimeStamp(reqObj);

        /* Get action */
        String actionName = getRequestActionName(reqObj);

        String s3ObjKey = getS3RequestObjectKey(clientTypeMajor, clientTimeCreated, engineId,
                                                reqTimestmap, actionName);

        /* Generate content string */
        final String content = gson.toJson(reqObj);
        final byte[] contentBytes = content.getBytes();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentBytes.length);
        metadata.setContentType("application/json");
        metadata.setContentEncoding(CONTENT_ENCODING.toString());

        final InputStream inputStream = new ByteArrayInputStream(content.getBytes(CONTENT_ENCODING));

        final PutObjectRequest putObjReq = new PutObjectRequest(s3BucketName, s3ObjKey, inputStream, metadata);

        if (s3Client == null) {
            logger.severe("Aborting publishing due to s3Client == null");
            return null;
        }

        String publishedObjKey = putObjReq.getKey();

        exe.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("Attempting to put object \"" + putObjReq.getKey() + " to bucket \"" + putObjReq.getBucketName() + "\"");
                    s3Client.putObject(putObjReq);
                } catch (AmazonServiceException exc) {
                    logger.severe("Encountered AmazonServiceException: \n\tmessage=\"" + exc.getMessage() + "\"" +
                            "\n\tstatus code=" + exc.getStatusCode() +
                            "\n\terror code="  + exc.getErrorCode() +
                            "\n\terror type="  + exc.getErrorType() +
                            "\n\trequest ID="  + exc.getRequestId());
                } catch (AmazonClientException exc) {
                    logger.severe("Encountered AmazonClientException: " + exc.getMessage());
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException exc) {}
                    }
                }
            }
        });

        return publishedObjKey;
    }

    /* Generate object keys for S3 */
    private static String getS3RequestObjectKey(final ClientTypeMajor clientTypeMajor,
                                                final Date clientTimeCreated,
                                                final String engineId,
                                                final Date reqTimestamp,
                                                final String actionName) {
        final String link = "_";

        String objKey = clientTypeMajor.toString() + link +
                        s3ObjKeyDateFormat.format(clientTimeCreated) + link +
                        engineId + link +
                        s3ObjKeyDateFormat.format(reqTimestamp) + link +
                        actionName + OBJECT_KEY_SUFFIX;

        return objKey;
    }

    /* Obtain the UUID of the handwriting engine */
    private String getRequestEngineId(final JsonObject reqObj) {
        String engineId = null;
        try {
            engineId = reqObj.get(ENGINE_ID_KEY).getAsString();
        } catch (Exception exc) {
            if (reqObj.has(ADDITIONAL_CLIENT_DATA_KEY)) {
                try {
                    engineId = reqObj.get(ADDITIONAL_CLIENT_DATA_KEY).getAsJsonObject().get(ENGINE_ID_KEY).getAsString();
                } catch (Exception exc1) {

                }
            }
        }

        return engineId;
    }

    private String getRequestActionName(final JsonObject reqObj) {
        final String unknownActionName = "unknown-action";

        if (reqObj.has("action")) {
            return reqObj.get("action").getAsString();
        } else {
            return unknownActionName;
        }
    }

    private Date getRequestTimeStamp(final JsonObject reqObj) {
        Date timestamp = null;
//        if (reqObj.has("timestamp")) {
//            String dateStr = reqObj.get("timestamp").getAsString();
//            try {
//                timestamp = dateFormat.parse(dateStr);
//            } catch (ParseException exc) {
//                timestamp = new Date();
//            }
//        } else {
        timestamp = new Date(); // Don't trust time stamps from the client
//        }

        return timestamp;
    }

    private Date engineId2TimeClientCreated(final String engineId) {
        if (!workersClientInfo.containsKey(engineId)) {
            throw new IllegalArgumentException("workersClientInfo map does not contains the specified engine ID: \"" + engineId + "\"");
        }
        WorkerClientInfo workerClientInfo = workersClientInfo.get(engineId);

        Date timeCreated = workerClientInfo.getTimeCreated();

        if (timeCreated == null) {
            throw new IllegalStateException("Encountered null value in created timestamp: engineId=\"" + engineId + "\"");
        }

        return timeCreated;
    }

    private ClientTypeMajor engineId2ClientTypeMajor(final String engineId) {
        if (!workersClientInfo.containsKey(engineId)) {
            throw new IllegalArgumentException("workersClientInfo map does not contains the specified engine ID: \"" + engineId + "\"");
        }
        WorkerClientInfo workerClientInfo = workersClientInfo.get(engineId);

        ClientTypeMajor clientTypeMajor = workerClientInfo.getClientTypeMajor();

        return clientTypeMajor;
    }

    private boolean isPublishBlocked(final ClientTypeMajor clientTypeMajor) {
        return noPublishClientTypeMajorList.contains(clientTypeMajor);
    }

    /* Obtain s3 bucket name for request loggin */
    private PlatoRequestPublisherS3Config getPlatoRequestPublisherS3Config() {
        Properties platoAwsProps = PropertiesHelper.getNestedProperties("awsPropertiesFile");

        final String awsS3BucketNameForRequestLogging = platoAwsProps.getProperty("awsS3BucketNameForRequestLogging");
        final String awsAccessKey = platoAwsProps.getProperty("awsAccessKey");
        final String awsSecretKey = platoAwsProps.getProperty("awsSecretKey");

        logger.info("S3 bucket name for request logging: \"" + awsS3BucketNameForRequestLogging + "\"");

        return new PlatoRequestPublisherS3Config(awsS3BucketNameForRequestLogging,
                                                 awsAccessKey,
                                                 awsSecretKey);
    }
}
