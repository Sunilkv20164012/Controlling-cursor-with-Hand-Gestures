"""
HandNetMaker.py
This script is only to be runs ONCE.
It creates the JSON model file for the neural network which will be used to
train and creature the neural network.
"""

from keras.engine.saving import model_from_json
from keras.models import Sequential
from keras.utils import plot_model
from keras.layers.core import Dense, Dropout, Activation, Flatten
from keras.layers import Conv2D, ZeroPadding2D, MaxPooling2D
import time


def custom_model_hand():
    # USER CODE STARTS HERE

    image_model = Sequential()
    image_model.add(ZeroPadding2D((2, 2), batch_input_shape=(1, 50, 50, 1)))

    # 54x54 fed in due to zero padding
    image_model.add(Conv2D(8, (5, 5), activation='relu', name='conv1_1'))
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(8, (5, 5), activation='relu', name='conv1_2'))

    image_model.add(MaxPooling2D((2, 2), strides=(2, 2)))  # convert 50x50 to 25x25

    # 25x25 fed in
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(16, (5, 5), activation='relu', name='conv2_1'))
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(16, (5, 5), activation='relu', name='conv2_2'))

    image_model.add(MaxPooling2D((5, 5), strides=(5, 5)))  # convert 25x25 to 5x5

    # 5x5 fed in
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(40, (5, 5), activation='relu', name='conv3_1'))
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(32, (5, 5), activation='relu', name='conv3_2'))

    image_model.add(Dropout(0.2))

    image_model.add(Flatten())

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))
    image_model.add(Dropout(0.2))

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))
    image_model.add(Dropout(0.15))

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))
    image_model.add(Dropout(0.1))

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))

    image_model.add(Dense(512))
    image_model.add(Activation('tanh'))

    image_model.add(Dense(10))
    image_model.add(Activation('sigmoid'))

    # USER CODE ENDS HERE

    return image_model


def make_model(filename):
    print("==================================================")

    print("Creating Model At: ", filename)
    start_time = time.time()
    model = custom_model_hand()

    json_model = model.to_json()

    with open(filename, "w") as json_file_name:
        json_file_name.write(json_model)

    end_time = time.time()
    total_time = end_time - start_time
    print("Model Created: ", total_time, " seconds")

    print("==================================================")


if __name__ == "__main__":
    make_model("hand_detection_model_3.json")

    json_file = open("hand_detection_model_3.json", 'r')
    loaded_model_json = json_file.read()
    json_file.close()
    plot_model(model_from_json(loaded_model_json), to_file='model.png', show_shapes=True, show_layer_names=True)
