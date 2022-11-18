cd ./bin
START java server.ChatCoordinator 5555 5556 5557 5558 5559
sleep 2
java client.ChatClient localhost 5555