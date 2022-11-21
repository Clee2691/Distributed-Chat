cd ./bin
START java server.ChatCoordinator ../config/port-list.cfg
sleep 2
java client.ChatClient localhost ../config/port-list.cfg
