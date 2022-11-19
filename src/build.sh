# only cleanup
if [ "$1" == "--rm" ]
then
  rm -rf ../bin/
  read -p "Press enter to continue."
  exit
fi

# run client docker container with cmd args
javac -d ../bin ./client/*.java ./gui/*.java ./server/*.java ./paxos/*.java

read -p "Build done! Press enter to continue."
exit