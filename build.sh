# only cleanup
if [ "$1" == "--rm" ]
then
  rm -rf ./bin/
  read -p "Press enter to continue."
  exit
fi

# run client docker container with cmd args
javac -d ./bin ./src/client/*.java ./src/gui/*.java ./src/server/*.java ./src/paxos/*.java

read -p "Press enter to continue."
exit