module purge
module load courses/cs555
gradle build
clear
java -cp ./build/libs/csx55.threads.jar csx55.threads.Registry $1