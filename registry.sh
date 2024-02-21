module purge
module load courses/cs455
gradle build
clear
java -cp ./build/libs/csx55.overlay.jar csx55.overlay.Registry $1