CLIENT_IMAGE='keyvalueclient_image'
PROJECT_NETWORK='store-network'

# Server container name must match from deploy.sh!
SERVER_CONTAINER='kvServer'

if [ $# -ne 2 ]
then
  echo "Usage: ./run_client.sh <client-container-name> <port-number>"
  read -p "Press enter to continue."
  exit
fi

# run client docker container with cmd args
docker run -it --rm --name "$1" \
 --network $PROJECT_NETWORK $CLIENT_IMAGE \
 java client.KeyValClient $SERVER_CONTAINER "$2" 2> /dev/null

read -p "Press enter to continue."