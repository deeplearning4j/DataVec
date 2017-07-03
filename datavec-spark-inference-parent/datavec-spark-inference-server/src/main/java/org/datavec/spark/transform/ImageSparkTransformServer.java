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
import org.nd4j.shade.jackson.databind.ObjectMapper;
import play.Mode;
import play.libs.Json;
import play.routing.RoutingDsl;
import play.server.Server;

import java.io.File;
import java.io.IOException;

import static play.mvc.Controller.request;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

/**
 * Created by kepricon on 17. 6. 19.
 */
@Slf4j
@Data
public class ImageSparkTransformServer extends SparkTransformServer {
    private ImageSparkTransform transform;

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
            ImageTransformProcess transformProcess = ImageTransformProcess.fromJson(json);
            transform = new ImageSparkTransform(transformProcess);
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
                return ok(Json.toJson(transform.getImageTransformProcess()));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        //return the host information for a given id
        routingDsl.POST("/transformprocess").routeTo(FunctionUtil.function0((() -> {
            try {
                ImageTransformProcess transformProcess = ImageTransformProcess.fromJson(getJsonText());
                setImageTransformProcess(transformProcess);
                log.info("Transform process initialized");
                return ok(Json.toJson(transformProcess));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        //return the host information for a given id
        routingDsl.POST("/transformincrementalarray").routeTo(FunctionUtil.function0((() -> {
            try {
                SingleImageRecord record = objectMapper.readValue(getJsonText(), SingleImageRecord.class);
                if (record == null)
                    return badRequest();
                return ok(Json.toJson(transformIncrementalArray(record)));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        //return the host information for a given id
        routingDsl.POST("/transformarray").routeTo(FunctionUtil.function0((() -> {
            try {
                BatchImageRecord batch = objectMapper.readValue(getJsonText(), BatchImageRecord.class);
                if (batch == null)
                    return badRequest();
                return ok(Json.toJson(transformArray(batch)));
            } catch (Exception e) {
                e.printStackTrace();
                return internalServerError();
            }
        })));

        server = Server.forRouter(routingDsl.build(), Mode.DEV, port);
    }

    @Override
    public void setCSVTransformProcess(TransformProcess transformProcess) {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    @Override
    public void setImageTransformProcess(ImageTransformProcess imageTransformProcess) {
        this.transform = new ImageSparkTransform(imageTransformProcess);
    }

    @Override
    public TransformProcess getCSVTransformProcess() {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    @Override
    public ImageTransformProcess getImageTransformProcess() {
        return transform.getImageTransformProcess();
    }

    @Override
    public SingleCSVRecord transformIncremental(SingleCSVRecord singleCsvRecord) {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    @Override
    public BatchCSVRecord transform(BatchCSVRecord batchCSVRecord) {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    @Override
    public Base64NDArrayBody transformArray(BatchCSVRecord batchCSVRecord) {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    @Override
    public Base64NDArrayBody transformArrayIncremental(SingleCSVRecord singleCsvRecord) {
        throw new UnsupportedOperationException("Invalid operation for " + this.getClass());
    }

    @Override
    public Base64NDArrayBody transformIncrementalArray(SingleImageRecord record) throws IOException {
        return transform.toArray(record);
    }

    @Override
    public Base64NDArrayBody transformArray(BatchImageRecord batch) throws IOException {
        return transform.toArray(batch);
    }

    public static void main(String[] args) throws Exception {
        new ImageSparkTransformServer().runMain(args);
    }
}
