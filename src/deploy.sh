PROJECT_NETWORK='store-network'
SERVER_IMAGE='keyvalueserver_image'
# Same SERVER_CONTAINER must match in run_client.sh
SERVER_CONTAINER='kvServer'
CLIENT_IMAGE='keyvalueclient_image'
PORT1=5555
PORT2=5556
PORT3=5557
PORT4=5558
PORT5=5559

# Can add more ports here


# clean up existing resources, if any
echo "----------Cleaning up existing resources----------"
docker rm -f $SERVER_CONTAINER 2> /dev/null
docker rmi -f $SERVER_IMAGE $CLIENT_IMAGE 2> /dev/null
docker network rm $PROJECT_NETWORK 2> /dev/null

# only cleanup
if [ "$1" == "--clean-only" ]
then
  read -p "Press enter to continue."
  exit
fi

# create a custom virtual network
echo "----------creating a virtual network----------"
docker network create $PROJECT_NETWORK

# build the images from Dockerfile
echo "----------Building images----------"
docker build -t $CLIENT_IMAGE --target client-build .
docker build -t $SERVER_IMAGE --target server-build .

# Run the image and open the required ports
# Add any extra ports from above if added with -p PORT:PORT
# and to the java command below
# MUST HAVE AT LEAST 5 PORTS!
echo "----------Running Server ----------"
docker run -d \
-p $PORT1:$PORT1 -p $PORT2:$PORT2 -p $PORT3:$PORT3 -p $PORT4:$PORT4 -p $PORT5:$PORT5 \
--name $SERVER_CONTAINER \
--network $PROJECT_NETWORK \
$SERVER_IMAGE \
java server.KVCoordinator $PORT1 $PORT2 $PORT3 $PORT4 $PORT5 2> /dev/null

read -p "Press enter to continue."

# echo "----------watching logs from server----------"
# docker logs $SERVER_CONTAINER -f