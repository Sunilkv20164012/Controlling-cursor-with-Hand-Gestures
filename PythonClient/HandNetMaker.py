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

    image_model = Sequential()

    # ZeroPadding2D layer can add rows and columns of zeros at the top, bottom, left and right side of an image tensor.
    # tuple of 2 ints: interpreted as two different symmetric padding values for height and width: (symmetric_height_pad, symmetric_width_pad)
    # "channels_last" corresponds to inputs with shape (batch, height, width, channels)
    # convolution channels (aka. depth or filters)
    image_model.add(ZeroPadding2D((2, 2), batch_input_shape=(1, 50, 50, 1)))

    # 54x54 fed in due to zero padding
    # kernel_size: An integer or tuple/list of 2 integers, specifying the height and width of the 2D convolution window.
    image_model.add(Conv2D(filters=8, kernel_size=(5, 5), activation='relu', name='conv1_1'))
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(filters=8, kernel_size=(5, 5), activation='relu', name='conv1_2'))
    # pool_size: integer or tuple of 2 integers, factors by which to downscale (vertical, horizontal). (2, 2) will halve the input in both spatial dimension.
    # strides: Integer, tuple of 2 integers, or None. Strides values. If None, it will default to pool_size.
    image_model.add(MaxPooling2D(pool_size=(2, 2), strides=(2, 2)))  # convert 50x50 to 25x25


    # 25x25 fed in
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(filters=16, kernel_size=(5, 5), activation='relu', name='conv2_1'))
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(filters=16, kernel_size=(5, 5), activation='relu', name='conv2_2'))

    image_model.add(MaxPooling2D(pool_size=(5, 5), strides=(5, 5)))  # convert 25x25 to 5x5


    # 5x5 fed in
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(filters=40, kernel_size=(5, 5), activation='relu', name='conv3_1'))
    image_model.add(ZeroPadding2D((2, 2)))
    image_model.add(Conv2D(filters=32, kernel_size=(5, 5), activation='relu', name='conv3_2'))

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
