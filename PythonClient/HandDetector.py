"""
This script can train the neural network AND run it for predictions.
"""

import NetLoader as NetLoader  # Custom net loader script
import DataLoader as DataLoader  # Custom data loader script
import EasySocket as EasySocket  # Custom socket script
import numpy as np

if __name__ == "__main__":

    model_path = "hand_detection_model_3.json"  # Mode file path
    weights_path = "hand_detection_weights_3.h5"  # Weights file path

    res_x = 50
    res_y = 50

    # MUST BE THE SAME LOCATION AS THE PATH IN HandGesture.java
    real_time_path = "../real_time.png"

    raw_input_size = 0  # there are no real input so set to 0
    raw_output_size = 10

    train_mode = False
    prediction_mode = False

    # Can be removed if there is no folder with gesture data 
    image_val_data_location_1_1 = "../HandGestureData/Ack/"
    image_val_data_location_2_1 = "../HandGestureData/Fist/"
    image_val_data_location_3_1 = "../HandGestureData/Hand/"
    image_val_data_location_4_1 = "../HandGestureData/One/"
    image_val_data_location_5_1 = "../HandGestureData/Straight/"
    image_val_data_location_6_1 = "../HandGestureData/Palm/"
    image_val_data_location_7_1 = "../HandGestureData/Thumbs/"
    image_val_data_location_8_1 = "../HandGestureData/None/"
    image_val_data_location_9_1 = "../HandGestureData/Swing/"
    image_val_data_location_10_1 = "../HandGestureData/Peace/"

    net = NetLoader.NetLoader(model_file=model_path, weights_file=weights_path,
                              learning_rate=0.001, decay_rate=0.00000001,
                              create_file=True, epoch_save=1)


    data_list = [  # First data set (Positional Gestures)
        image_val_data_location_1_1, image_val_data_location_2_1,
        image_val_data_location_3_1, image_val_data_location_4_1,
        image_val_data_location_5_1, image_val_data_location_6_1,
        image_val_data_location_7_1, image_val_data_location_8_1,
        image_val_data_location_9_1, image_val_data_location_10_1]


    # if training mode is false, then data list can just be an empty array
    if not train_mode:
        data_list = []

    # create data loader
    # if data_list is an empty array then calling set_elements_to_train will be an error 
    data = DataLoader.DataLoader(data_paths=data_list, size_x=res_x,
                                 size_y=res_y, num_inputs=raw_input_size,
                                 num_outputs=raw_output_size, black_white=True)


    # # just for testing purpose
    # raw_RGB = data.load_image(real_time_path)  # loader raw image
    # raw_RGB = np.array(raw_RGB, dtype=np.float32)
    #
    # pre = net.predict(np.array([raw_RGB]))
    # for p in pre:
    #     for i in p:
    #         print(i*100, end="  ")
    # exit(0)
    # # testing ends here

    # if in training mode
    if train_mode:

        data.combine_data(random_sort=True)
        input_element_1, output_element_1 = data.get_set_elements_to_train(0)

        if prediction_mode:
            pre = net.predict(input_element_1[0])
            for i in range(len(pre)):
                pred = pre[i] * 100
                print("Max Index: " + str(np.argmax(pred)) + "  Output: " + str(int(pred[0])) + " " + str(
                    int(pred[1])) + " " + str(int(pred[2])) + " " + str(int(pred[3])) + " " + str(int(pred[4]))
                      + " " + str(int(pred[5])) + " " + str(int(pred[6])) + " " + str(int(pred[7])) + " " + str(
                            int(pred[8])) + " " + str(int(pred[9])))
        else:
            for i in range(15):  # Originally it was 15
                net.fit(input_element_1[0], output_element_1, verbose=2)

    # if training mode is false
    else:
        socket = EasySocket.EasySocket(
            preset_unpack_types=['i'])  # add a preset type of 1 integer (get that float value)

        socket.connect()  # connect to server

        while True:
            if socket.get_anything(4, 0):  # get the integer (it does not make a difference what it gets)

                raw_RGB = data.load_image(real_time_path)
                while raw_RGB == "saurabh":
                    raw_RGB = data.load_image(real_time_path)  # loader raw image
                raw_RGB = np.array(raw_RGB, dtype=np.float32)

                pre = net.predict(np.array([raw_RGB]))  # get prediction
                # print(pre)
                # [[ 0.94716406  0.00541257  0.02063853  0.00590561  0.01073392  0.003591  0.00479027  0.02207533  0.00283904  0.00311629]]

                # create a line of message
                message = ""
                for i in range(len(pre[0])):
                    message += str(pre[0][i])
                    if i == len(pre[0]) - 1:
                        message += "\n"
                    else:
                        message += " "

                socket.send_string_data(message)  # send prediction message to server

        socket.close()  # close socket if while loop breaks....which it never will lol
