package org.datavec.spark.transform;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.datavec.api.transform.TransformProcess;
import org.datavec.image.transform.ImageTransformProcess;
import org.datavec.spark.transform.model.*;
import org.datavec.spark.transform.service.DataVecTransformService;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import play.Mode;
import play.libs.Json;
import play.routing.RoutingDsl;
import play.server.Server;

import java.io.File;
import java.io.IOException;

import static play.mvc.Controller.request;
import static play.mvc.Results.*;

/**
 * A rest server for using an
 * {@link TransformProcess} based on simple
 * csv values and a schema via REST.
 *
 * The input values are an {@link SingleCSVRecord}
 * which (based on the input schema) will automatically
 * have their values transformed.
 *
 * @author Adam Gibson
 */
@Slf4j
@Data
public class CSVSparkTransformServer extends SparkTransformServer {
    private CSVSparkTransform transform;

    public void runMain(String[] args) throws Exception {
        JCommander jcmdr = new JCommander(this);

        try {
            jcmdr.parse(args);
        } catch (ParameterException e) {
            //User provides invalid input -> print the usage info
            jcmdr.usage();
            if (jsonPath == null)
                System.err.println("Json path parameter is missing.");
            try {
                Thread.sleep(500);
            } catch (Exception e2) {
            }
            System.exit(1);
        }

        RoutingDsl routingDsl = new RoutingDsl();


        if (jsonPath != null) {
            String json = FileUtils.readFileToString(new File(jsonPath));
            TransformProcess transformProcess = TransformProcess.fromJson(json);
            transform = new CSVSparkTransform(transformProcess);
        } else {
            log.warn("Server started with no json for transform process. Please ensure you specify a transform process via sending a post request with raw json"
                            + "to /transformprocess");
        }


        //return the host information for a given id
        routingDsl.GET("/transformprocess").routeTo(FunctionUtil.function0((() -> {
            try {
                if (transform == null)
                    return badRequest();
                log.info("Transform process initialized");
                return ok(Json.toJson(transform.getTransformProcess()));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        //return the host information for a given id
        routingDsl.POST("/transformprocess").routeTo(FunctionUtil.function0((() -> {
            try {
                TransformProcess transformProcess = TransformProcess.fromJson(getJsonText());
                setCSVTransformProcess(transformProcess);
                log.info("Transform process initialized");
                return ok(Json.toJson(transformProcess));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        //return the host information for a given id
        routingDsl.POST("/transformincremental").routeTo(FunctionUtil.function0((() -> {
            try {
                SingleCSVRecord record = objectMapper.readValue(getJsonText(), SingleCSVRecord.class);
                if (record == null)
                    return badRequest();
                return ok(Json.toJson(transformIncremental(record)));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        //return the host information for a given id
        routingDsl.POST("/transform").routeTo(FunctionUtil.function0((() -> {
            try {
                BatchCSVRecord batch = transform(objectMapper.readValue(getJsonText(), BatchCSVRecord.class));
                if (batch == null)
                    return badRequest();
                return ok(Json.toJson(batch));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        routingDsl.POST("/transformincrementalarray").routeTo(FunctionUtil.function0((() -> {
            try {
                SingleCSVRecord record = objectMapper.readValue(getJsonText(), SingleCSVRecord.class);
                if (record == null)
                    return badRequest();
                return ok(Json.toJson(transformArrayIncremental(record)));
            } catch (Exception e) {
                return internalServerError();
            }
        })));

        routingDsl.POST("/transformarray").routeTo(FunctionUtil.function0((() -> {
            try {
                BatchCSVRecord batchCSVRecord = objectMapper.readValue(getJsonText(), BatchCSVRecord.class);
                if (batchCSVRecord == null)
                    return badRequest();
                return ok(Json.toJson(transformArray(batchCSVRecord)));
            } catch (Exception e) {
                return internalServerError();
            }
        })));

        server = Server.forRouter(routingDsl.build(), Mode.DEV, port);
    }

    public static void main(String[] args) throws Exception {
        new CSVSparkTransformServer().runMain(args);
    }

    /**
     * @param transformProcess
     */
    @Override
    public void setCSVTransformProcess(TransformProcess transformProcess) {
        this.transform = new CSVSparkTransform(transformProcess);
    }

    @Override
    public void setImageTransformProcess(ImageTransformProcess imageTransformProcess) {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    /**
     * @return
     */
    @Override
    public TransformProcess getCSVTransformProcess() {
        return transform.getTransformProcess();
    }

    @Override
    public ImageTransformProcess getImageTransformProcess() {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    /**
     * @param transform
     * @return
     */
    @Override
    public SingleCSVRecord transformIncremental(SingleCSVRecord transform) {
        return this.transform.transform(transform);
    }

    /**
     * @param batchCSVRecord
     * @return
     */
    @Override
    public BatchCSVRecord transform(BatchCSVRecord batchCSVRecord) {
        return transform.transform(batchCSVRecord);
    }

    /**
     * @param batchCSVRecord
     * @return
     */
    @Override
    public Base64NDArrayBody transformArray(BatchCSVRecord batchCSVRecord) {
        try {
            return this.transform.toArray(batchCSVRecord);
        } catch (IOException e) {
            throw new IllegalStateException("Transform array shouldn't throw exception");
        }
    }

    /**
     * @param singleCsvRecord
     * @return
     */
    @Override
    public Base64NDArrayBody transformArrayIncremental(SingleCSVRecord singleCsvRecord) {
        try {
            return this.transform.toArray(singleCsvRecord);
        } catch (IOException e) {
            throw new IllegalStateException("Transform array shouldn't throw exception");
        }
    }

    @Override
    public Base64NDArrayBody transformIncrementalArray(SingleImageRecord singleImageRecord) throws IOException {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    @Override
    public Base64NDArrayBody transformArray(BatchImageRecord batchImageRecord) throws IOException {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }
}
