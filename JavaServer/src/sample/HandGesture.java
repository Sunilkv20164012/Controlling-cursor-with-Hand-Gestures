package sample;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class HandGesture {

    private String[] gestureTypes = new String[]{"Ack", "Fist", "Hand", "One", "Straight", "Palm", "Thumbs", "None", "Swing", "Peace", "TestK"}; //types of gestures
    private int gestureIndex = 10; //Gesture index to collect data for

    // Image collection information
    private int imageNumber = 0; //current image number being taken
    private int maxImageNumber = 500; //max image numbers
    private int imageCounter = 0; // wait timer before it equals reset count
    private int resetCount = 1; // ticks to wait before image is taken


    //Data writing into files
    private String pathToDataCollection = "/home/saurabh/Desktop/FinalYearProject/HandGestureData/TestK/";
    private PrintWriter writer; //writer to write to file

    private Rectangle boxPosition; //Red box location

    public HandGesture() {

        //location where raw data would be created upon data collection being true
        String rawDataFilename = pathToDataCollection + "raw_data.txt";
        File rawData = new File(rawDataFilename); //raw data file

        // if it does not exist
        if (!rawData.exists()) {

            //create new raw data file
            try {
                rawData.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // create writer to be able to write to raw data file
        try {
            writer = new PrintWriter(rawData);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public List<BufferedImage> paint(BufferedImage initialWebcamImage, int[] pixelRaster, int width, int height, boolean dataCollectionMode, boolean clicked) {

        List<BufferedImage> bufferedImages = new ArrayList<>();
        //min and max bounds of the detected box
        int minX = 10000;
        int maxX = -10000;
        int minY = 10000;
        int maxY = -10000;

        Rectangle handBound = null; //hand bound location

        BufferedImage tempInitialWebcamImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); //temporary webcam image
        BufferedImage newImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB); //50px by 50px image that will be fed into the neural network

        Vector<Rectangle> listOfFoundObjects = new Vector<>(); //list of found objects

        //Initialize rasters
        int[] tempRaster = new int[width * height]; //temp raster

        int[][] pixelRaster2D = new int[height][width]; //converting pixelRaster to 2D format to check for surrounding pixels
        int[][] tempRaster2D = new int[height][width]; //temp raster for initial image
        int[][] densityRaster = new int[height][width]; //raster for density
        int[][] clusterRaster = new int[height][width]; //raster for cluster

        int index = 0; //used to access pixel raster when running through 2D array

        //Increase image contrast
        RescaleOp op = new RescaleOp(2f, 0, null); //incerase contract by 2 times the scale factor
        initialWebcamImage = op.filter(initialWebcamImage, initialWebcamImage); //use filter to update the camera image

        //get rasters
        initialWebcamImage.getRGB(0, 0, width, height, pixelRaster, 0, width); // get pixel raster
        initialWebcamImage.getRGB(0, 0, width, height, tempRaster, 0, width); //get temp raster


        //First pass, get all skin pixel
        for (int i = 0; i < height; i++) {

            for (int j = 0; j < width; j++, index++) {

                tempRaster2D[i][j] = pixelRaster[index];

                int[] color = hexToRGB(pixelRaster[index]); //convert hex arbg integer to RGB array

                float[] hsb = new float[3]; // HSB array
                Color.RGBtoHSB(color[0], color[1], color[2], hsb); //convert RGB to HSB array

                // Initial pass will use strict skin pixel rule.
                // It will only find skin pixels within smaller section compared to loose pixel rule
                // This will help avoid impurities in the detection
                if (strictSkinPixelRule(hsb)) {
                    pixelRaster2D[i][j] = 0xFFFFFFFF; //if found turn pixel white in the 2D array
                } else {
                    pixelRaster2D[i][j] = 0xFF000000; //else turn pixel black in the 2D array
                }
            }
        }


        //Creating a 2D density raster of found initial skin pixels
        //Run through pixel raster 2D array
        for (int col = 0; col < height; col++) {
            for (int row = 0; row < width; row++) {

                //IF pixel is white
                if (pixelRaster2D[col][row] == 0xFFFFFFFF) {

                    //calculate pixel boundary (needed if the pixel is near the edges)
                    int max = 10;
                    int lowY = col - max >= 0 ? col - max : 0;
                    int highY = col + max < height ? col + max : height - 1;

                    int lowX = row - max >= 0 ? row - max : 0;
                    int highX = row + max < width ? row + max : width - 1;

                    //Run through pixels all pixels, at max 10 pixels away from this pixel in a square shape
                    for (int i = lowY; i <= highY; i++) {
                        for (int j = lowX; j <= highX; j++) {
                            if (pixelRaster2D[i][j] == 0xFFFFFFFF) {
                                //both work, but i feel like densityRaster[col][row] is a little better
                                densityRaster[i][j]++;
                                //densityRaster[col][row]++; //update desnity of  if pixel found is white
                            }
                        }

                    }
                }
            }
        }

        //Now we can use that initial pass to find the general location of the hand in the image
        for (int col = 0; col < height; col++) {
            for (int row = 0; row < width; row++) {

                pixelRaster2D[col][row] = 0xFF000000; //make pixel black, since it should not be based upon the density raster

                //if density at this pixel is greater then 60
                if (densityRaster[col][row] > 60) {

                    pixelRaster2D[col][row] = 0xFFFFFFFF; //turn this pixel white

                    boolean intersects = false; //check if any rectangles intersect with the one about to be created

                    Rectangle rect = new Rectangle(row - 7, col - 7, 14, 14); //this pixel's rectangle

                    // check of any previous created rectagles intersect with new rectangle
                    for (Rectangle listOfFoundObject : listOfFoundObjects) {
                        //rectangle does intersect
                        if (rect.intersects(listOfFoundObject)) {
                            intersects = true; //if a rectangle is found, then this pixel needs to ignored
                            break;
                        }
                    }

                    // If no intersection found
                    if (!intersects) {
                        listOfFoundObjects.addElement(rect); //if no rectangles are found, then this rectangle can be added to the list

                        // Update to see if there is a new top left or bottom right corner with this new rectangle
                        if (minX > rect.x)
                            minX = rect.x;

                        if (maxX < rect.x + rect.width)
                            maxX = rect.x + rect.width;

                        if (minY > rect.y)
                            minY = rect.y;

                        if (maxY < rect.y + rect.height)
                            maxY = rect.y + rect.height;
                    }
                }
            }
        }

        // if there is at least 1 rectangle found
        if (listOfFoundObjects.size() > 0) {

            //Fix the top left and bottom right location to be exactly 100 pixel by 100 pixel in in size

            //Fix x axis
            if (maxX - minX > 100) {
                int diff = (maxX - minX) - 100;
                int half = diff / 2;
                minX += half;
                maxX -= half;

            } else if (maxX - minX < 100) {
                int diff = 100 - (maxX - minX);
                int half = diff / 2;
                minX -= half;
                maxX += half;
            }

            //Fix y axis
            if (maxY - minY > 100) {

                int diff = (maxY - minY) - 100;
                int half = diff / 2;
                minY += half;
                maxY -= half;

            } else if (maxY - minY < 100) {
                int diff = 100 - (maxY - minY);
                int half = diff / 2;
                minY -= half;
                maxY += half;
            }

            //Fix bounds to be within the camera image
            if (minX < 0)
                minX = 0;
            if (minY < 0)
                minY = 0;

            if (maxX >= width)
                maxX = width - 1;
            if (maxY >= height)
                maxY = height - 1;

            handBound = new Rectangle(minX, minY, maxX - minX, maxY - minY); //create hand bound location

            // Creating cluster raster
            for (int col = minY; col < maxY; col++) {
                for (int row = minX; row < maxX; row++) {

                    //if pixel is white
                    if (pixelRaster2D[col][row] == 0xFFFFFFFF) {

                        int max = 5;
                        int lowY = col - max >= 0 ? col - max : 0;
                        int highY = col + max < height ? col + max : height - 1;

                        int lowX = row - max >= 0 ? row - max : 0;
                        int highX = row + max < width ? row + max : width - 1;

                        // run through all pixels, 5 pixels away from this pixel
                        for (int i = lowY; i <= highY; i++) {
                            for (int j = lowX; j <= highX; j++) {
                                clusterRaster[i][j]++; //increase clustering
                            }
                        }
                    }
                }
            }

            //Now that the hand bound has been found.
            //Cluster raster can be used to fill in the missing pixels.
            for (int col = minY; col < maxY; col++) {
                for (int row = minX; row < maxX; row++) {

                    //If cluster density is greater than 10 and this pixel is black.
                    //It must mean that this pixel is near another white pixel!
                    if (clusterRaster[col][row] > 10 && pixelRaster2D[col][row] == 0xFF000000) {

                        int[] color = hexToRGB(tempRaster2D[col][row]);

                        float[] hsb = new float[3];
                        Color.RGBtoHSB(color[0], color[1], color[2], hsb);

                        // Use loose skin pixel rule to check if this pixel is with in a certain range to be called a skin pixel
                        if (looseSkinPixelRule(hsb)) {
                            pixelRaster2D[col][row] = 0xFFFFFFFF; //turn it white
                        }
                    }
                }
            }

            //Copy pixel raster 2D into pixel raster 1D
            index = 0;
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++, index++) {
                    pixelRaster[index] = pixelRaster2D[i][j];
                }
            }


            // Set initial webcam image to the pixel raster
            initialWebcamImage.setRGB(0, 0, width, height, pixelRaster, 0, width);

            //crop hand from the pixel raster
            BufferedImage crop = cropImage(initialWebcamImage, handBound);

            //Now the pixel raster image needs to be drawn on to the new image and be scaled down to 50px by 50px
            Graphics2D g = newImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            //draw cropped image on to new image
            g.drawImage(crop, 0, 0, 50, 50, 0, 0, crop.getWidth(), crop.getHeight(), null);
            g.dispose(); //dispose graphics as it is not needed
        }


        // if hand is hand bound is null, which means no hand is found
        if (handBound == null) {

            //make a simple black image
            Graphics g2 = newImage.getGraphics();
            g2.setColor(Color.black);
            g2.fillRect(0, 0, 50, 50);
            g2.dispose();  //dispose graphics as it is not needed

        }

        //If data collection mode is true and the user clicked on the window screen
        if (dataCollectionMode && clicked) {

            // if max number of images a taken
            if (imageNumber >= maxImageNumber) {
                clicked = false;
                writer.close(); //close print writer
            }
            // if max number of images are not taken yet
            else {

                //if image counter equals reset count, it's time to taken an image
                if (imageCounter == resetCount) {

                    System.out.println(imageNumber); //print current image number being taken
                    int i = 0;

                    // write 0's up to the gesture index and stop right before it
                    for (; i < gestureIndex; i++) {
                        if (i == gestureTypes.length - 1)
                            writer.print("0");
                        else
                            writer.print("0 ");
                    }

                    // if current gesture type is the last index
                    if (i == gestureTypes.length - 1) {
                        //write a 1
                        writer.print("1");
                        i++;
                    }
                    // else there is more gestures left
                    else {

                        writer.print("1 "); //write a 1
                        i++;

                        //write the rest of 0's
                        for (; i < gestureTypes.length; i++) {
                            if (i == gestureTypes.length - 1)
                                writer.print("0");
                            else
                                writer.print("0 ");
                        }
                    }

                    //save this image
                    try {
                        ImageIO.write(newImage, "png", new File(pathToDataCollection + imageNumber + ".png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                    imageCounter = 0;
                    imageNumber++;

                    //if this is not the last image write a new line character
                    if (imageNumber < maxImageNumber) {
                        writer.print("\n");
                    }
                }

                imageCounter++;
            }
        }

        //set temp initial webcam image to temp raster
        tempInitialWebcamImage.setRGB(0, 0, width, height, tempRaster, 0, width);

        Graphics2D graphics2D = tempInitialWebcamImage.createGraphics();

        //draw green pixel boxes from the density raster
        graphics2D.setColor(Color.green);
        for (Rectangle rect : listOfFoundObjects) {
//            System.out.println(rect.x + " " + rect.y + " " + rect.width + " " + rect.height);
//            System.out.println("            " + (rect.x + 20 + width + 10) + " " + (rect.y + 40) + " " + rect.width + " " + rect.height);
            graphics2D.drawRect(rect.x, rect.y, rect.width, rect.height);
        }

        //draw bound hand if it exists
        if (handBound != null) {
            boxPosition = handBound;
            graphics2D.setColor(Color.red);
            graphics2D.drawRect(handBound.x, handBound.y, handBound.width, handBound.height);
            graphics2D.dispose();
        }


        bufferedImages.add(tempInitialWebcamImage);
        bufferedImages.add(initialWebcamImage);
        bufferedImages.add(newImage);
        return bufferedImages;

    }


    /*
     * Strict skin pixel detection.
     * A small range of skin detection.
     */
    private boolean strictSkinPixelRule(float[] hsb) {
        return hsb[0] < 0.15f && hsb[1] > 0.2f && hsb[1] < 0.63f;
    }

    /*
     * Loose skin pixel detection.
     * A broader range values for the skin pixel.
     */
    private boolean looseSkinPixelRule(float[] hsb) {
        return hsb[0] < 0.4f && hsb[1] < 1f && hsb[2] < 0.7f;
    }

    /*
     * Returns a cropped image
     * @param src Source image
     * @param rect Bounds
     * @return New image cropped based on bounds
     */
    private BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        return src.getSubimage(rect.x, rect.y, rect.width, rect.height);
    }

    /*
     * Converts hex to integer array which contains red, green, and blue color of 0-255.
     * @param rgbHex integer in format of 0xAARRGGBB, A = alpha, R = red, G = green, B = blue
     */
    private int[] hexToRGB(int argbHex) {
        int[] rgb = new int[3];

        rgb[0] = (argbHex & 0xFF0000) >> 16; //get red
        rgb[1] = (argbHex & 0xFF00) >> 8; //get green
        rgb[2] = (argbHex & 0xFF); //get blue

        return rgb;//return array
    }

}
