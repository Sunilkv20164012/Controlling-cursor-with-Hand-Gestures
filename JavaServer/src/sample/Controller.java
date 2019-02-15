package sample;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.github.sarxos.webcam.Webcam;


public class Controller implements Initializable {

    @FXML
    Button btnStartCamera;
    @FXML
    Button btnStopCamera;
    @FXML
    Button btnDisposeCamera;
    @FXML
    ComboBox<WebCamInfo> cbCameraOptions;
    @FXML
    BorderPane bpWebCamPaneHolder;
    @FXML
    FlowPane fpBottomPane;
    @FXML
    ImageView imgWebCamCapturedImage;
    @FXML
    ImageView topRightImage;
    @FXML
    ImageView middleRightImage;
    @FXML
    VBox gestureTypesVBox;


    private int width, height; //height and width of camera
    private BufferedImage grabbedImage; // initial cam image
    private Webcam selWebCam = null;
    private boolean stopCamera = false;
    private ObjectProperty<Image> imageProperty = new SimpleObjectProperty<>();
    private ObjectProperty<Image> secondImageProperty = new SimpleObjectProperty<>();
    private ObjectProperty<Image> thirdImageProperty = new SimpleObjectProperty<>();
    private int[] pixelRaster; //pixel raster for initial cam image

    private String[] gestureTypes = new String[]{"Ack", "Fist", "Hand", "One", "Straight", "Palm", "Thumbs", "None", "Swing", "Peace", "TestK"}; //types of gestures

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {

        fpBottomPane.setDisable(true);
        ObservableList<WebCamInfo> options = FXCollections.observableArrayList();
        int webCamCounter = 0;
        for (Webcam webcam : Webcam.getWebcams()) {
            WebCamInfo webCamInfo = new WebCamInfo();
            webCamInfo.setWebCamIndex(webCamCounter);
            webCamInfo.setWebCamName(webcam.getName());
            options.add(webCamInfo);
            webCamCounter++;
        }
        cbCameraOptions.setItems(options);
        cbCameraOptions.setPromptText("Choose Camera");
        cbCameraOptions.getSelectionModel().selectedItemProperty().addListener((arg01, arg11, arg2) -> {
            if (arg2 != null) {

                System.out.println("WebCam Index: " + arg2.getWebCamIndex() + ": WebCam Name:" + arg2.getWebCamName());
                initializeWebCam(arg2.getWebCamIndex());
            }
        });
        Platform.runLater(new Runnable() {

            @Override
            public void run() {

                setImageViewSize();

            }
        });

    }

    private void setImageViewSize() {

        double height = bpWebCamPaneHolder.getHeight();
        double width = bpWebCamPaneHolder.getWidth();
        imgWebCamCapturedImage.setFitHeight(height);
        imgWebCamCapturedImage.setFitWidth(width);
        imgWebCamCapturedImage.prefHeight(height);
        imgWebCamCapturedImage.prefWidth(width);
        imgWebCamCapturedImage.setPreserveRatio(true);
        System.out.println("Width is " + width + " Height is " + height);
    }

    private void initializeWebCam(final int webCamIndex) {

        Task<Void> webCamInitializer = new Task<Void>() {

            @Override
            protected Void call() {

                if (selWebCam == null) {
                    selWebCam = Webcam.getWebcams().get(webCamIndex);
                    selWebCam.open();
                } else {
                    closeCamera();
                    selWebCam = Webcam.getWebcams().get(webCamIndex);
                    selWebCam.open();

                }
                width = selWebCam.getViewSize().width;
                height = selWebCam.getViewSize().height;
                System.out.println("Width " + width + " Height " + height);

                //initialize image buffer and pixel raster initialized according to buffer size
                grabbedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                pixelRaster = ((DataBufferInt) grabbedImage.getRaster().getDataBuffer()).getData();
                startWebCamStream(false);
                return null;
            }

        };

        new Thread(webCamInitializer).start();
        fpBottomPane.setDisable(false);

    }

    private void startWebCamStream(boolean clicked) {
        //Data collection mode or mouse control mode
        //If data collection mode is false then it will go into hand gesture prediction mode (Python client will need to connect to this server)
        boolean dataCollectionMode = false;
        boolean mouseControlMode = true;
        int gestureIndex = 10; //Gesture index to collect data for

        stopCamera = false;
        HandGesture handGesture = new HandGesture(dataCollectionMode, mouseControlMode, gestureTypes, gestureIndex);
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() {

                while (!stopCamera) {
                    try {
                        if ((grabbedImage = selWebCam.getImage()) != null) {

                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    List<BufferedImage> bufferedImage = handGesture.paint(grabbedImage, pixelRaster, width, height, clicked);

                                    Image mainImage = SwingFXUtils.toFXImage(bufferedImage.get(0), null);
                                    imageProperty.set(mainImage);

                                    Image secondImage = SwingFXUtils.toFXImage(bufferedImage.get(1), null);
                                    secondImageProperty.set(secondImage);

                                    Image thirdImage = SwingFXUtils.toFXImage(bufferedImage.get(2), null);
                                    thirdImageProperty.set(thirdImage);

                                    gestureTypesVBox.getChildren().clear();
                                    int[] guess = handGesture.getGuess();
                                    for (int i = 0; i < gestureTypes.length; i++) {
                                        String gesture = gestureTypes[i];
                                        HBox hBox = new HBox();
                                        hBox.setSpacing(10);
                                        hBox.getChildren().add(new Label(gesture));
                                        //hBox.setSpacing(5);

                                        ProgressBar p2 = new ProgressBar();
                                        p2.setProgress((float)guess[i]/100);
                                        hBox.getChildren().add(p2);
                                        //hBox.setSpacing(10);

                                        hBox.getChildren().add(new Label(Integer.toString(guess[i])));
                                        //hBox.setSpacing(15);

                                        gestureTypesVBox.getChildren().add(hBox);
                                        gestureTypesVBox.setSpacing(20);
                                    }
                                }
                            });

                            grabbedImage.flush();

                        }
                    } catch (Exception ignored) {
                        System.out.println("Error stop webCam");
                    }
                }

                return null;

            }

        };
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        imgWebCamCapturedImage.imageProperty().bind(imageProperty);
        topRightImage.imageProperty().bind(secondImageProperty);
        middleRightImage.imageProperty().bind(thirdImageProperty);

    }


    private void closeCamera() {
        if (selWebCam != null) {
            selWebCam.close();
        }
    }

    @FXML
    public void startCamera(ActionEvent event) {
        stopCamera = false;
        startWebCamStream(true);
        btnStartCamera.setDisable(true);
        btnStopCamera.setDisable(false);
    }

    @FXML
    public void stopCamera(ActionEvent event) {
        stopCamera = true;
        btnStartCamera.setDisable(false);
        btnStopCamera.setDisable(true);
    }

    @FXML
    public void disposeCamera(ActionEvent event) {
        stopCamera = true;
        closeCamera();
        Webcam.shutdown();
        btnStopCamera.setDisable(true);
        btnStartCamera.setDisable(true);
    }
}
