# http://www.doiserbia.nb.rs/img/doi/1450-698X/2017/1450-698X1794101V.pdf
# Potential framework for this project, though they compute euler angles and not quaternions.

# https://www.mdpi.com/1424-8220/15/8/19302
# Means of representing IMU data into a quaternion.

# https://stanford.edu/class/ee267/lectures/lecture10.pdf
# Understanding the various steps needed to compute a quaternion from basic IMU data

# https://www.ashwinnarayan.com/post/how-to-integrate-quaternions/
# Doing math with quaternions

import numpy as np
import quaternion
import math




#Assume sensor data can be stored as a numpy array of size 3
#For now we ignore GPS data but that needs to be incorporated to account for long lat changes in relative az and alt of celestial bodies 
#This contains all the different distinct sensor types that show up. Not all (like the linear acceleration sensor) might be used
accel = np.array([0,0,0], dtype=np.float64)
magnet = np.array([0,0,0], dtype=np.float64)
gravity = np.array([0,0,0], dtype=np.float64)
rotVect = np.array([0,0,0], dtype=np.float64)
linAcc = np.array([0,0,0], dtype=np.float64)
gyro = np.array([0,0,0], dtype=np.float64)
orient = np.array([0,0,0], dtype=np.float64)

quaternion = np.quaternion(0,0,0,0)



#Do we really want to use quaternions? I'm considering using euler angles as that will (hopefully) allows a really easy way to correct any deviations of the telescope

#As it turns out determining even heading is difficult with three dimensions to deal with.

def convertToGauss(data):
    newArray = np.array([data[0] * .01, data[1]*.01, data[2]*.01], dtype= np.float64)
    return newArray

def parseMagnet(data):
    data = convertToGauss(data)
    x, y, z = data[0], data[1], data[2]


    if (y > 0):
        heading = 90 - np.arctan(x/y) * (180/math.pi)
    elif( y < 0):
        heading = np.arctan(x/y) * (180/math.pi)
    elif( y == 0 and x < 0):
        heading = 180
    elif(y == 0 and x > 0):
        heading = 0
    return heading

def main():
    userIn = input("Please enter numbers sperated by a space: ")
    userList = userIn.split(" ")
    userArray = np.asarray(userList, dtype=np.float64);
    print(userArray);

    print(parseMagnet(userArray))





main();


