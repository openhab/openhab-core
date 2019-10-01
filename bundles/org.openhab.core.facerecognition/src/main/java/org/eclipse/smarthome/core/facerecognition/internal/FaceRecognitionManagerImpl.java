/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.facerecognition.internal;

import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgcodecs.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.config.core.ConfigurableService;
import org.eclipse.smarthome.core.facerecognition.FaceRecognitionManager;
import org.eclipse.smarthome.core.library.types.RawType;
import org.openhab.core.facerecognition.dto.FaceRecognitionResult;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service provides functionality to detect a set of trained faces in an image
 *
 * @author Philipp Meisberger - Initial contribution
 */
@Component(name = "org.openhab.facerecognition", property = { Constants.SERVICE_PID + "=org.openhab.facerecognition",
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=system",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Face Recognition",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=" + FaceRecognitionManagerImpl.CONFIG_URI })
@NonNullByDefault
public class FaceRecognitionManagerImpl implements FaceRecognitionManager {
    // constants for the configuration properties
    protected final static String CONFIG_URI = "system:facerecognition";
    private static final int UNKNOWN_USER_LABEL = -1;
    private static final String FACE_RECOGNITON_FOLDER = "facerecognition";
    private static final String HAARCASCADE_FILE = "haarcascade_frontalface_default.xml";
    private static final String MODELS_FILE = "models.xml";
    private static final String CONFIG_THRESHOLD = "threshold";
    private static final String UNKNOWN_USER_NAME = "UNKNOWN";

    private final Logger logger = LoggerFactory.getLogger(FaceRecognitionManagerImpl.class);
    private final FaceRecognizer faceRecognizer;
    private @Nullable CascadeClassifier faceCascade = null;
    private final File modelsFile;
    private int threshold = 80;

    @Activate
    public FaceRecognitionManagerImpl() {
        faceRecognizer = createLBPHFaceRecognizer();
        File faceRecognitionFolder = new File(
                ConfigConstants.getUserDataFolder() + File.separator + FACE_RECOGNITON_FOLDER);
        faceRecognitionFolder.mkdirs();

        // Load models file
        modelsFile = new File(faceRecognitionFolder, MODELS_FILE);
        if (modelsFile.exists()) {
            faceRecognizer.load(MODELS_FILE);
        }

        // Load face cascade
        Bundle bundle = FrameworkUtil.getBundle(getClass());
        try (InputStream stream = bundle.getResource(FACE_RECOGNITON_FOLDER + "/" + HAARCASCADE_FILE).openStream()) {
            File haarCascadeFile = new File(faceRecognitionFolder, HAARCASCADE_FILE);
            if (!haarCascadeFile.exists()) {
                FileOutputStream os = new FileOutputStream(haarCascadeFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = stream.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                stream.close();
                os.close();
            }
            faceCascade = new CascadeClassifier(haarCascadeFile.getAbsolutePath());
            if (faceCascade.empty()) {
                throw new IOException("Error loading cascade classifier from :" + haarCascadeFile.getAbsolutePath());
            }
            logger.debug("Loaded cascade classifier from '{}'", haarCascadeFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("The resource '{}/{}' could not be loaded : ", FACE_RECOGNITON_FOLDER, HAARCASCADE_FILE,
                    e.getMessage());
        }
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        modified(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        threshold = ((BigDecimal) config.get(CONFIG_THRESHOLD)).intValue();
    }

    @Deactivate
    protected void deactivate() {
        if (faceCascade != null) {
            faceCascade.close();
        }
        faceRecognizer.close();
    }

    @Override
    public List<FaceRecognitionResult> recognize(byte[] imageData) {
        List<FaceRecognitionResult> results = new ArrayList<>();
        // Convert image to gray-scale
        Mat imageDataMat = new Mat(imageData);
        Mat grayScaleImage = imdecode(imageDataMat, CV_LOAD_IMAGE_GRAYSCALE);
        RectVector faces = new RectVector();
        // Detect face in image
        if (faceCascade != null) {
            faceCascade.detectMultiScale(grayScaleImage, faces);
            // Try to find known face in image
            for (int i = 0; i < faces.size(); i++) {

                // Extract face from image
                Mat face = new Mat(grayScaleImage, faces.get(i));

                IntPointer label = new IntPointer(1);
                DoublePointer score = new DoublePointer(1);

                boolean facesTrained = modelsFile.exists();
                if (facesTrained) {
                    // Try to recognize user
                    faceRecognizer.predict(face, label, score);
                } else {
                    // Set default unknown user values
                    label.put(UNKNOWN_USER_LABEL);
                    score.put(Double.MAX_VALUE);
                    logger.debug("No faces trained yet");
                }

                FaceRecognitionResult result = new FaceRecognitionResult();
                result.recognized = facesTrained && (score.get(0) <= threshold);
                result.score = score.get(0);
                result.labelInfo = result.recognized ? faceRecognizer.getLabelInfo(label.get(0)).getString()
                        : label.get(0) == UNKNOWN_USER_LABEL ? UNKNOWN_USER_NAME
                                : faceRecognizer.getLabelInfo(label.get(0)).getString();
                result.image = result.recognized ? readBuffer(face) : readBuffer(new Mat(face));
                results.add(result);
            }
        }
        return results;
    }

    RawType readBuffer(Mat face) {
        ByteBuffer outputBuffer = ByteBuffer.allocate(face.arraySize());
        imencode(".jpg", face, outputBuffer);
        return new RawType(outputBuffer.array(), "image/jpeg");
    }

    @Override
    public void train(String name, RawType picture) {
        MatVector images = new MatVector(1);
        Mat accessDeniedFace = imdecode(new Mat(new BytePointer(picture.getBytes())), IMREAD_UNCHANGED);
        images.put(0, accessDeniedFace);

        // Setup label
        Mat labels = new Mat(1, 1, CV_32SC1);
        IntBuffer labelsBuffer = labels.createBuffer();
        int label = name.toLowerCase().hashCode();
        labelsBuffer.put(0, label);

        // Train face
        faceRecognizer.update(images, labels);
        faceRecognizer.setLabelInfo(label, name);
        faceRecognizer.save(MODELS_FILE);
        logger.debug("Trained face of user '{}'", name);

    }

}
